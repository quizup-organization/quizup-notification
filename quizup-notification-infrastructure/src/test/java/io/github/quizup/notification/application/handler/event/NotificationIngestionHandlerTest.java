package io.github.quizup.notification.application.handler.event;

import io.github.quizup.matchmaking.domain.event.ChallengeEvent;
import io.github.quizup.matchmaking.domain.event.RoomEvent;
import io.github.quizup.matchmaking.domain.model.ChallengeRoomId;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.social.domain.event.UserFollowerEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Ingestion des événements de domaine : défis (inbox) et salles (pas de notification d'inbox —
 * seul {@code ROOM_COMPLETED} rattache le {@code gameId} à l'acceptation).
 *
 * <p>La création est idempotente même après un **hard delete** : une relecture Kafka
 * at-least-once ne doit pas tenter de recréer un agrégat existant (poison pill).</p>
 */
class NotificationIngestionHandlerTest {

    private static final Instant AT = Instant.parse("2026-10-03T18:02:00Z");

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
    private final NotificationPreferenceRepositoryPort preferenceRepository = mock(NotificationPreferenceRepositoryPort.class);
    private final EventStore eventStore = mock(EventStore.class);
    private final DomainEventStream absentAggregate = mock(DomainEventStream.class);
    private final NotificationIngestionHandler handler = new NotificationIngestionHandler(
            commandGateway, notificationRepository, preferenceRepository, eventStore);

    @BeforeEach
    void setUp() {
        when(eventStore.readEvents(anyString())).thenReturn(absentAggregate);
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
    void challengeAccepted_expiresInvitationAndNotifiesChallengerWithRoomId() {
        handler.on(new ChallengeEvent.ChallengeAcceptedEvent(
                "challenge-1", "topic-1", "challenger", "opponent", AT));

        verify(notificationRepository).expireChallengeInvitations("challenge-1", AT);

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("challenger");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.ROOM_ACCEPTED);
        assertThat(captor.getValue().actorId()).isEqualTo("opponent");
        assertThat(captor.getValue().sourceId()).isEqualTo(ChallengeRoomId.of("challenge-1"));
        assertThat(captor.getValue().topicId()).isEqualTo("topic-1");
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
    void challengeCancelled_expiresTheInvitationWithoutNotification() {
        handler.on(new ChallengeEvent.ChallengeCancelledEvent("challenge-1", "challenger", AT));

        verify(notificationRepository).expireChallengeInvitations("challenge-1", AT);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void challengeExpired_expiresTheInvitationWithoutNotification() {
        handler.on(new ChallengeEvent.ChallengeExpiredEvent("challenge-1", AT));

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
    void roomCompleted_attachesGameIdToAcceptance() {
        handler.on(new RoomEvent.RoomCompletedEvent("room-1", "game-1", AT));

        verify(notificationRepository).attachGameId("room-1", "game-1");
        verifyNoInteractions(commandGateway);
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
