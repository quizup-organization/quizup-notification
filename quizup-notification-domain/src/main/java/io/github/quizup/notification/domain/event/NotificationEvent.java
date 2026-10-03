package io.github.quizup.notification.domain.event;

import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationType;

import java.time.Instant;

public interface NotificationEvent {

    String notificationId();

    record NotificationCreatedEvent(
            String notificationId,
            String userId,
            NotificationType type,
            String actorId,
            String sourceId,
            String topicId,
            String gameId,
            Instant expiresAt,
            Instant createdAt
    ) implements NotificationEvent {
    }

    record NotificationReadEvent(
            String notificationId,
            Instant readAt
    ) implements NotificationEvent {
    }
}
