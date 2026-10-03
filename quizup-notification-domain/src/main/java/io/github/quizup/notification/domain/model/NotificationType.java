package io.github.quizup.notification.domain.model;

/**
 * Type d'une notification personnelle. La catégorie de préférence associée est
 * {@link NotificationCategory}.
 */
public enum NotificationType {
    FOLLOW,
    LOBBY_INVITATION,
    LOBBY_ACCEPTED,
    LOBBY_DECLINED,
    LOBBY_CANCELLED,
    LOBBY_EXPIRED;

    public NotificationCategory category() {
        return switch (this) {
            case FOLLOW -> NotificationCategory.FOLLOW;
            default -> NotificationCategory.LOBBY;
        };
    }
}
