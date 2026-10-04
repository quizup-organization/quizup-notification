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

    /**
     * La notification est supprimée par son destinataire : le read model est purgé et l'agrégat
     * marqué supprimé ({@code AggregateLifecycle.markDeleted()}) — l'event store conserve
     * l'historique. {@code userId} porte le destinataire pour le routage du push.
     */
    record NotificationDeletedEvent(
            String notificationId,
            String userId,
            Instant deletedAt
    ) implements NotificationEvent {
    }
}
