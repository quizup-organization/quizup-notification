package io.github.quizup.notification.domain.model;

/**
 * Type d'une notification personnelle. La catégorie de préférence associée est
 * {@link NotificationCategory}.
 */
public enum NotificationType {
    FOLLOW,
    /** Défi nominatif reçu (intention asynchrone, avant toute salle). */
    CHALLENGE_RECEIVED,
    /** Défi nominatif refusé par l'invité. */
    CHALLENGE_DECLINED,
    /** Salle acceptée : la partie existe (deep link arène une fois la salle purgée). */
    ROOM_ACCEPTED;

    public NotificationCategory category() {
        return switch (this) {
            case FOLLOW -> NotificationCategory.FOLLOW;
            default -> NotificationCategory.ROOM;
        };
    }
}
