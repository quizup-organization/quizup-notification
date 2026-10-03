package io.github.quizup.notification.domain.model;

import java.time.Instant;

/** Préférence d'un joueur pour une catégorie de notification (défaut : activée). */
public record NotificationPreference(
        String userId,
        NotificationCategory category,
        boolean enabled,
        Instant updatedAt
) {
}
