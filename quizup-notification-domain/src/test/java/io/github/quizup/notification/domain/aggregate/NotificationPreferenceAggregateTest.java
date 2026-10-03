package io.github.quizup.notification.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.event.NotificationPreferenceUpdatedEvent;
import io.github.quizup.notification.domain.model.NotificationCategory;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

class NotificationPreferenceAggregateTest {

    private static final String USER = "user-1";

    private final AggregateTestFixture<NotificationPreferenceAggregate> fixture =
            new AggregateTestFixture<>(NotificationPreferenceAggregate.class);

    @Test
    void update_createsAggregateIfMissing() {
        fixture.givenNoPriorActivity()
                .when(new NotificationCommand.UpdateNotificationPreferenceCommand(
                        USER, NotificationCategory.LOBBY, false))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        NotificationPreferenceUpdatedEvent.class,
                        e -> !((NotificationPreferenceUpdatedEvent) e).enabled()
                                && ((NotificationPreferenceUpdatedEvent) e).category()
                                == NotificationCategory.LOBBY));
    }

    @Test
    void update_onExistingAggregate_appliesEvent() {
        fixture.given(new NotificationPreferenceUpdatedEvent(
                        USER, NotificationCategory.LOBBY, false, java.time.Instant.now()))
                .when(new NotificationCommand.UpdateNotificationPreferenceCommand(
                        USER, NotificationCategory.LOBBY, true))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        NotificationPreferenceUpdatedEvent.class,
                        e -> ((NotificationPreferenceUpdatedEvent) e).enabled()));
    }
}
