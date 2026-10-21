package io.github.quizup.notification.application.handler.event;

import io.github.quizup.identity.domain.event.UserEvent;
import io.github.quizup.matchmaking.domain.event.ChallengeEvent;
import io.github.quizup.matchmaking.domain.event.RoomEvent;
import io.github.quizup.matchmaking.domain.model.ChallengeRoomId;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationPreference;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.social.domain.event.UserFollowerEvent;
import io.github.quizup.theme.domain.event.QuestionReportEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    void accountSuspended_isMandatoryEvenWhenAccountPreferenceDisabled() {
        when(preferenceRepository.find("user-1", NotificationCategory.ACCOUNT))
                .thenReturn(Optional.of(new NotificationPreference(
                        "user-1", NotificationCategory.ACCOUNT, false, AT)));

        handler.on(new UserEvent.UserSuspendedEvent(
                "user-1", "user@quizup.dev", "suspension-1",
                io.github.quizup.identity.domain.model.UserSuspensionReason.HARASSMENT,
                "internal", "admin-1", AT, null));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("user-1");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.ACCOUNT_SUSPENDED);
        assertThat(captor.getValue().sourceId()).isEqualTo("suspension-1");
        assertThat(captor.getValue().actorId()).isNull();
    }

    @Test
    void questionReported_notifiesTheTopicCreator() {
        handler.on(new QuestionReportEvent.QuestionReportedEvent(
                "thread-1", "report-1", "question-1", "topic-1", "creator-1",
                "reporter-1", "game-1", java.util.Set.of(
                        io.github.quizup.theme.domain.model.QuestionReportReason.ANSWERS),
                "Bonne réponse fausse", null, null, AT));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("creator-1");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.QUESTION_REPORTED);
        assertThat(captor.getValue().actorId()).isEqualTo("reporter-1");
        assertThat(captor.getValue().sourceId()).isEqualTo("report-1");
        assertThat(captor.getValue().topicId()).isEqualTo("topic-1");
    }

    @Test
    void questionReported_withoutTopicCreator_doesNothing() {
        handler.on(new QuestionReportEvent.QuestionReportedEvent(
                "thread-1", "report-1", "question-1", "topic-1", null,
                "reporter-1", "game-1", java.util.Set.of(
                        io.github.quizup.theme.domain.model.QuestionReportReason.ANSWERS),
                null, null, null, AT));

        verifyNoInteractions(commandGateway);
    }

    @Test
    void challengeCreated_notifiesTheOpponent() {        handler.on(new ChallengeEvent.ChallengeCreatedEvent(
                "challenge-1", "topic-1", "challenger", "opponent", null,
                AT.plusSeconds(3600), AT));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("opponent");
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.CHALLENGE_RECEIVED);
        assertThat(captor.getValue().sourceId()).isEqualTo("challenge-1");
        assertThat(captor.getValue().gameId()).isNull();
    }

    @Test
    void challengeCreated_rematchCarriesSourceGameId() {
        handler.on(new ChallengeEvent.ChallengeCreatedEvent(
                "challenge-1", "topic-1", "challenger", "opponent", "game-1",
                AT.plusSeconds(10), AT));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().gameId()).isEqualTo("game-1");
        assertThat(captor.getValue().expiresAt()).isEqualTo(AT.plusSeconds(10));
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
    void challengeCancelled_deletesTheInvitation() {
        Notification invitation = Notification.builder()
                .notificationId("notification-1")
                .userId("opponent")
                .type(NotificationType.CHALLENGE_RECEIVED)
                .actorId("challenger")
                .sourceId("challenge-1")
                .createdAt(AT)
                .build();
        when(notificationRepository.findBySourceIdAndType("challenge-1", NotificationType.CHALLENGE_RECEIVED))
                .thenReturn(List.of(invitation));

        handler.on(new ChallengeEvent.ChallengeCancelledEvent("challenge-1", "challenger", AT));

        ArgumentCaptor<NotificationCommand.DeleteNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.DeleteNotificationCommand.class);
        verify(commandGateway).sendAndWait(captor.capture());
        assertThat(captor.getValue().notificationId()).isEqualTo("notification-1");
        assertThat(captor.getValue().userId()).isEqualTo("opponent");
    }

    @Test
    void challengeCancelled_withoutInvitation_doesNothing() {
        when(notificationRepository.findBySourceIdAndType("challenge-1", NotificationType.CHALLENGE_RECEIVED))
                .thenReturn(List.of());

        handler.on(new ChallengeEvent.ChallengeCancelledEvent("challenge-1", "challenger", AT));

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
        verify(notificationRepository).expireBySourceIdAndType("room-1", NotificationType.WIZZ, AT);
        verifyNoInteractions(commandGateway);
    }

    @Test
    void roomWizzed_replacesPreviousEntryAndNotifiesTheTarget() {
        Notification previous = Notification.builder()
                .notificationId("notification-previous")
                .userId("opponent")
                .type(NotificationType.WIZZ)
                .actorId("initiator")
                .sourceId("room-1")
                .createdAt(AT.minusSeconds(20))
                .build();
        when(notificationRepository.findBySourceIdAndType("room-1", NotificationType.WIZZ))
                .thenReturn(List.of(previous));

        handler.on(new RoomEvent.RoomWizzedEvent("room-1", "initiator", "opponent", AT));

        ArgumentCaptor<NotificationCommand.DeleteNotificationCommand> deleteCaptor =
                ArgumentCaptor.forClass(NotificationCommand.DeleteNotificationCommand.class);
        verify(commandGateway).sendAndWait(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().notificationId()).isEqualTo("notification-previous");
        assertThat(deleteCaptor.getValue().userId()).isEqualTo("opponent");

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> createCaptor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway).send(createCaptor.capture());
        assertThat(createCaptor.getValue().userId()).isEqualTo("opponent");
        assertThat(createCaptor.getValue().type()).isEqualTo(NotificationType.WIZZ);
        assertThat(createCaptor.getValue().actorId()).isEqualTo("initiator");
        assertThat(createCaptor.getValue().sourceId()).isEqualTo("room-1");
    }

    @Test
    void roomWizzed_secondDeliveryReusesTheSameDeterministicId() {
        handler.on(new RoomEvent.RoomWizzedEvent("room-1", "initiator", "opponent", AT));
        handler.on(new RoomEvent.RoomWizzedEvent("room-1", "initiator", "opponent", AT));

        ArgumentCaptor<NotificationCommand.CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(NotificationCommand.CreateNotificationCommand.class);
        verify(commandGateway, times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).notificationId())
                .isEqualTo(captor.getAllValues().get(1).notificationId());
    }

    @Test
    void roomEntered_expiresTheWizzOfTheArrivingPlayer() {
        handler.on(new RoomEvent.RoomEnteredEvent("room-1", "opponent", null, AT));

        verify(notificationRepository).expireBySourceIdAndTypeAndUserId(
                "room-1", NotificationType.WIZZ, "opponent", AT);
    }

    @Test
    void roomCancelled_expiresPendingWizz() {
        handler.on(new RoomEvent.RoomCancelledEvent("room-1", "initiator", "PLAYER_CANCELLED", AT));

        verify(notificationRepository).expireBySourceIdAndType("room-1", NotificationType.WIZZ, AT);
    }

    @Test
    void roomExpired_expiresPendingWizz() {
        handler.on(new RoomEvent.RoomExpiredEvent("room-1", AT));

        verify(notificationRepository).expireBySourceIdAndType("room-1", NotificationType.WIZZ, AT);
    }

    @Test
    void roomFailed_expiresPendingWizz() {
        handler.on(new RoomEvent.RoomFailedEvent("room-1", "GAME_CREATION_TIMEOUT", AT));

        verify(notificationRepository).expireBySourceIdAndType("room-1", NotificationType.WIZZ, AT);
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
