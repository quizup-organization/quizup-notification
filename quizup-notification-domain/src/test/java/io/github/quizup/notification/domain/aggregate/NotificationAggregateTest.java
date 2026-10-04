package io.github.quizup.notification.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.event.NotificationEvent;
import io.github.quizup.notification.domain.exception.NotificationExceptions;
import io.github.quizup.notification.domain.model.NotificationType;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

class NotificationAggregateTest {

    private static final String NOTIFICATION_ID = "notif-1";
    private static final String USER = "user-1";

    private final AggregateTestFixture<NotificationAggregate> fixture =
            new AggregateTestFixture<>(NotificationAggregate.class);

    @Test
    void create_appliesCreatedEvent() {
        fixture.givenNoPriorActivity()
                .when(new NotificationCommand.CreateNotificationCommand(
                        NOTIFICATION_ID, USER, NotificationType.FOLLOW, "follower-1",
                        "follow-1", null, null, null))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        NotificationEvent.NotificationCreatedEvent.class,
                        e -> USER.equals(((NotificationEvent.NotificationCreatedEvent) e).userId())
                                && ((NotificationEvent.NotificationCreatedEvent) e).type() == NotificationType.FOLLOW));
    }

    @Test
    void create_withoutRecipient_isRejected() {
        fixture.givenNoPriorActivity()
                .when(new NotificationCommand.CreateNotificationCommand(
                        NOTIFICATION_ID, " ", NotificationType.FOLLOW, "follower-1",
                        "follow-1", null, null, null))
                .expectException(NotificationExceptions.MissingRecipientProblem.class);
    }

    @Test
    void markRead_byOwner_appliesReadEvent() {
        fixture.given(created())
                .when(new NotificationCommand.MarkNotificationReadCommand(NOTIFICATION_ID, USER))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        NotificationEvent.NotificationReadEvent.class,
                        e -> NOTIFICATION_ID.equals(((NotificationEvent.NotificationReadEvent) e).notificationId())));
    }

    @Test
    void markRead_byOther_isRejected() {
        fixture.given(created())
                .when(new NotificationCommand.MarkNotificationReadCommand(NOTIFICATION_ID, "user-2"))
                .expectException(NotificationExceptions.NotNotificationOwnerProblem.class);
    }

    @Test
    void markRead_isIdempotent() {
        fixture.given(created(), new NotificationEvent.NotificationReadEvent(NOTIFICATION_ID, java.time.Instant.now()))
                .when(new NotificationCommand.MarkNotificationReadCommand(NOTIFICATION_ID, USER))
                .expectNoEvents();
    }

    @Test
    void delete_byOwner_appliesDeletedEventAndMarksAggregateDeleted() {
        fixture.given(created())
                .when(new NotificationCommand.DeleteNotificationCommand(NOTIFICATION_ID, USER))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        NotificationEvent.NotificationDeletedEvent.class,
                        e -> NOTIFICATION_ID.equals(((NotificationEvent.NotificationDeletedEvent) e).notificationId())))
                .expectMarkedDeleted();
    }

    @Test
    void delete_byOther_isRejected() {
        fixture.given(created())
                .when(new NotificationCommand.DeleteNotificationCommand(NOTIFICATION_ID, "user-2"))
                .expectException(NotificationExceptions.NotNotificationOwnerProblem.class);
    }

    private static NotificationEvent.NotificationCreatedEvent created() {
        return new NotificationEvent.NotificationCreatedEvent(
                NOTIFICATION_ID, USER, NotificationType.FOLLOW, "follower-1",
                "follow-1", null, null, null, java.time.Instant.now());
    }
}
