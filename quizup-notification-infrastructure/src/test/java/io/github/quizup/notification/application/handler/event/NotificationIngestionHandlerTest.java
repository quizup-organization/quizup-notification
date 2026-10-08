package io.github.quizup.notification.application.handler.event;

import io.github.quizup.matchmaking.domain.event.ChallengeEvent;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.model.NotificationRouting;
import io.github.quizup.notification.domain.model.NotificationRoutingSource;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRoutingRepositoryPort;
import io.github.quizup.social.domain.event.UserFollowerEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Un défi qui n'est plus disponible n'envoie **plus** de notification « défi raté » : l'invitation
 * en attente est expirée dans le read model (le client masque Accepter/Refuser) et le routage est
 * purgé. Seul le refus explicite (`LOBBY_DECLINED`) reste notifié.
 *
 * <p>La création est idempotente même après un **hard delete** : une relecture Kafka
 * at-least-once ne doit pas tenter de recréer un agrégat existant (poison pill).</p>
 */
class NotificationIngestionHandlerTest {

    private static final Instant AT = Instant.parse("2026-10-03T18:02:00Z");

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
    private final NotificationPreferenceRepositoryPort preferenceRepository = mock(NotificationPreferenceRepositoryPort.class);
    private final NotificationRoutingRepositoryPort routingRepository = mock(NotificationRoutingRepositoryPort.class);
    private final EventStore eventStore = mock(EventStore.class);
    private final DomainEventStream absentAggregate = mock(DomainEventStream.class);
    private final NotificationIngestionHandler handler = new NotificationIngestionHandler(
            commandGateway, notificationRepository, preferenceRepository, routingRepository, eventStore);

    @BeforeEach
    void setUp() {
        when(eventStore.readEvents(anyString())).thenReturn(absentAggregate);
    }

    @Test
    void lobbyExpired_expiresInvitationWithoutNotification() {
        handler.on(new LobbyEvent.LobbyExpiredEvent("lobby-1", AT));

        verify(notificationRepository).expireInvitations("lobby-1", AT);
        verify(routingRepository).delete(NotificationRoutingSource.LOBBY, "lobby-1");
        verifyNoInteractions(commandGateway);
    }

    @Test
    void lobbyCancelled_expiresInvitationWithoutNotification() {
        handler.on(new LobbyEvent.LobbyCancelledEvent("lobby-2", "alice", "PLAYER_CANCELLED", AT));

        verify(notificationRepository).expireInvitations("lobby-2", AT);
        verify(routingRepository).delete(NotificationRoutingSource.LOBBY, "lobby-2");
        verifyNoInteractions(commandGateway);
    }

    @Test
    void lobbyDeclined_expiresInvitation() {
        handler.on(new LobbyEvent.LobbyDeclinedEvent("lobby-3", "alice", "bob", AT));

        verify(notificationRepository).expireInvitations("lobby-3", AT);
    }

    @Test
    void lobbyJoined_expiresInvitation() {
        handler.on(new LobbyEvent.LobbyJoinedEvent("lobby-4", "bob", AT));

        verify(notificationRepository).expireInvitations("lobby-4", AT);
    }

    @Test
    void lobbyFailed_expiresInvitation() {
        handler.on(new LobbyEvent.LobbyFailedEvent("lobby-5", "CREATE_GAME_FAILED", AT));

        verify(notificationRepository).expireInvitations("lobby-5", AT);
    }

    @Test
    void challengeCreated_notifiesTheOpponent() {
        handler.on(new ChallengeEvent.ChallengeCreatedEvent(
                "challenge-1", "topic-1", "challenger", "opponent",
                AT.plusSeconds(3600), AT));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("opponent");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.CHALLENGE_RECEIVED);
        assertThat(captor.getValue().sourceId()).isEqualTo("challenge-1");
    }

    @Test
    void challengeDeclined_expiresInvitationAndNotifiesChallenger() {
        handler.on(new ChallengeEvent.ChallengeDeclinedEvent(
                "challenge-1", "challenger", "opponent", AT));

        verify(notificationRepository).expireChallengeInvitations("challenge-1", AT);

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("challenger");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.CHALLENGE_DECLINED);
        assertThat(captor.getValue().actorId()).isEqualTo("opponent");
    }

    @Test
    void challengeAccepted_expiresTheInvitation() {
        handler.on(new ChallengeEvent.ChallengeAcceptedEvent(
                "challenge-1", "topic-1", "challenger", "opponent", AT));

        verify(notificationRepository).expireChallengeInvitations("challenge-1", AT);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void challengePurged_expiresTheInvitationWithoutNotification() {
        handler.on(new ChallengeEvent.ChallengePurgedEvent("challenge-1", AT));

        verify(notificationRepository).expireChallengeInvitations("challenge-1", AT);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void lobbyMissed_notifiesTheWaitingPlayer() {
        NotificationRouting routing = NotificationRouting.builder()
                .sourceType(NotificationRoutingSource.LOBBY)
                .sourceId("lobby-7")
                .initiatorId("absent")
                .opponentId("waiter")
                .participantId("waiter")
                .topicId("topic-1")
                .updatedAt(AT)
                .build();
        when(routingRepository.find(NotificationRoutingSource.LOBBY, "lobby-7"))
                .thenReturn(Optional.of(routing));

        handler.on(new LobbyEvent.LobbyMissedEvent("lobby-7", "absent", "OPPONENT_OFFLINE", AT));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("waiter");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.LOBBY_MISSED);
        assertThat(captor.getValue().actorId()).isEqualTo("absent");
    }

    @Test
    void lobbyCompleted_attachesGameIdToAcceptance() {
        handler.on(new LobbyEvent.LobbyCompletedEvent("lobby-6", "game-1", AT));

        verify(notificationRepository).expireInvitations("lobby-6", AT);
        verify(notificationRepository).attachGameId("lobby-6", "game-1");
    }

    @Test
    void followReplayedAfterDelete_isSkippedWhenAggregateExists() {
        DomainEventStream existingAggregate = mock(DomainEventStream.class);
        when(existingAggregate.hasNext()).thenReturn(true);
        when(eventStore.readEvents(anyString())).thenReturn(existingAggregate);

        handler.on(new UserFollowerEvent.UserFollowedEvent("follow-1", "actor-1", "user-1", AT));

        verifyNoInteractions(commandGateway);
    }

    @Test
    void followFirstDelivery_dispatchesCreateCommand() {
        handler.on(new UserFollowerEvent.UserFollowedEvent("follow-2", "actor-1", "user-1", AT));

        verify(commandGateway).send(any());
    }
}
