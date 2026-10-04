package io.github.quizup.notification.application.handler.event;

import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.notification.domain.model.NotificationRoutingSource;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRoutingRepositoryPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Un défi qui n'est plus disponible n'envoie **plus** de notification « défi raté » : l'invitation
 * en attente est expirée dans le read model (le client masque Accepter/Refuser) et le routage est
 * purgé. Seul le refus explicite (`LOBBY_DECLINED`) reste notifié.
 */
class NotificationIngestionHandlerTest {

    private static final Instant AT = Instant.parse("2026-10-03T18:02:00Z");

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
    private final NotificationPreferenceRepositoryPort preferenceRepository = mock(NotificationPreferenceRepositoryPort.class);
    private final NotificationRoutingRepositoryPort routingRepository = mock(NotificationRoutingRepositoryPort.class);
    private final NotificationIngestionHandler handler = new NotificationIngestionHandler(
            commandGateway, notificationRepository, preferenceRepository, routingRepository);

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
}
