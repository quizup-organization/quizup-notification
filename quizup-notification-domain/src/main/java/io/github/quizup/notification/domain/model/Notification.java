package io.github.quizup.notification.domain.model;

import lombok.Builder;

import java.time.Instant;

/**
 * Read model d'une notification personnelle. Les champs optionnels dépendent du
 * {@link NotificationType} : {@code actorId} (auteur), {@code sourceId} (salon/ticket/follow),
 * {@code topicId}, {@code gameId}, {@code expiresAt}.
 */
@Builder(toBuilder = true)
public record Notification(
        String notificationId,
        String userId,
        NotificationType type,
        String actorId,
        String sourceId,
        String topicId,
        String gameId,
        Instant expiresAt,
        Instant readAt,
        Instant createdAt
) {
    public boolean unread() {
        return readAt == null;
    }
}
