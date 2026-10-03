package io.github.quizup.notification.application.handler.event;

import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.model.NotificationRouting;
import io.github.quizup.notification.domain.model.NotificationRoutingSource;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRoutingRepositoryPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un défi nominatif expiré doit prévenir **les deux** joueurs : sinon l'invitation de l'invité
 * reste actionnable jusqu'à la purge du salon et provoque un 404 à l'acceptation.
 */
class NotificationIngestionHandlerTest {

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
    private final NotificationPreferenceRepositoryPort preferenceRepository = mock(NotificationPreferenceRepositoryPort.class);
    private final NotificationRoutingRepositoryPort routingRepository = mock(NotificationRoutingRepositoryPort.class);
    private final NotificationIngestionHandler handler = new NotificationIngestionHandler(
            commandGateway, notificationRepository, preferenceRepository, routingRepository);

    @Test
    void lobbyExpired_notifiesInitiatorAndInvitee() {
        when(routingRepository.find(NotificationRoutingSource.LOBBY, "lobby-1"))
                .thenReturn(Optional.of(NotificationRouting.builder()
                        .sourceType(NotificationRoutingSource.LOBBY)
                        .sourceId("lobby-1")
                        .initiatorId("alice")
                        .opponentId("bob")
                        .topicId("topic-1")
                        .updatedAt(Instant.parse("2026-10-03T18:00:00Z"))
                        .build()));
        when(notificationRepository.existsById(any())).thenReturn(false);
        when(preferenceRepository.find(any(), any())).thenReturn(Optional.empty());

        handler.on(new LobbyEvent.LobbyExpiredEvent("lobby-1", Instant.parse("2026-10-03T18:02:00Z")));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway, times(2)).send(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationCommand.CreateNotificationCommand::userId)
                .containsExactlyInAnyOrder("alice", "bob");
        assertThat(captor.getAllValues())
                .allMatch(command -> command.type() == NotificationType.LOBBY_EXPIRED);
        verify(routingRepository).delete(NotificationRoutingSource.LOBBY, "lobby-1");
    }

    @Test
    void lobbyExpired_withoutOpponent_notifiesInitiatorOnly() {
        when(routingRepository.find(NotificationRoutingSource.LOBBY, "lobby-2"))
                .thenReturn(Optional.of(NotificationRouting.builder()
                        .sourceType(NotificationRoutingSource.LOBBY)
                        .sourceId("lobby-2")
                        .initiatorId("alice")
                        .topicId("topic-1")
                        .updatedAt(Instant.parse("2026-10-03T18:00:00Z"))
                        .build()));
        when(notificationRepository.existsById(any())).thenReturn(false);
        when(preferenceRepository.find(any(), any())).thenReturn(Optional.empty());

        handler.on(new LobbyEvent.LobbyExpiredEvent("lobby-2", Instant.parse("2026-10-03T18:02:00Z")));

        verify(commandGateway, times(1)).send(any(NotificationCommand.CreateNotificationCommand.class));
    }
}
