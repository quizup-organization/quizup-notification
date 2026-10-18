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
    ROOM_ACCEPTED,
    /**
     * Relance (« wizz ») : un joueur présent dans la salle demande à son adversaire absent de le
     * rejoindre. Une seule entrée par salle — chaque relance remplace la précédente.
     */
    WIZZ,
    /** Signalement d'une question appartenant à un sujet du joueur. */
    QUESTION_REPORTED,
    /** Information obligatoire : le compte du joueur a été suspendu. */
    ACCOUNT_SUSPENDED;

    public NotificationCategory category() {
        return switch (this) {
            case FOLLOW -> NotificationCategory.FOLLOW;
            case QUESTION_REPORTED -> NotificationCategory.QUESTION;
            case ACCOUNT_SUSPENDED -> NotificationCategory.ACCOUNT;
            default -> NotificationCategory.ROOM;
        };
    }
}
