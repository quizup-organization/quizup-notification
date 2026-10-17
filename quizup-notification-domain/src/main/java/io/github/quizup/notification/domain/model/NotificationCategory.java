package io.github.quizup.notification.domain.model;

/** Catégorie de préférence de notification (un toggle côté Réglages). */
public enum NotificationCategory {
    FOLLOW,
    ROOM,
    /** Signalements de questions déposés sur les sujets du joueur. */
    QUESTION
}
