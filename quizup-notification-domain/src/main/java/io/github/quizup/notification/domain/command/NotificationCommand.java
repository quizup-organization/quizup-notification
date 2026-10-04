package io.github.quizup.notification.domain.command;

import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationType;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;

/**
 * Commandes du domaine notification. La création est déclenchée par l'ingestion des événements
 * de domaine (identifiant déterministe : relecture Kafka idempotente).
 */
public interface NotificationCommand {

    /** Création d'une notification pour un destinataire (commande système). */
    record CreateNotificationCommand(
            @TargetAggregateIdentifier String notificationId,
            String userId,
            NotificationType type,
            String actorId,
            String sourceId,
            String topicId,
            String gameId,
            Instant expiresAt
    ) implements NotificationCommand {
    }

    /** Marque une notification comme lue (seul son destinataire y est autorisé). */
    record MarkNotificationReadCommand(
            @TargetAggregateIdentifier String notificationId,
            String userId
    ) implements NotificationCommand {
    }

    /** Supprime une notification (hard delete ; seul son destinataire y est autorisé). */
    record DeleteNotificationCommand(
            @TargetAggregateIdentifier String notificationId,
            String userId
    ) implements NotificationCommand {
    }

    /** Marque toutes les notifications non lues d'un joueur comme lues (fan-out). */
    record MarkAllNotificationsReadCommand(
            String userId
    ) implements NotificationCommand {
    }

    /** Active/désactive une catégorie de notification pour un joueur. */
    record UpdateNotificationPreferenceCommand(
            @TargetAggregateIdentifier String userId,
            NotificationCategory category,
            boolean enabled
    ) implements NotificationCommand {
    }
}
