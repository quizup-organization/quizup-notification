package io.github.quizup.notification.domain.model;

import lombok.Builder;

import java.time.Instant;

/**
 * Index de routage des destinataires : les événements terminaux (Join/Cancel/Decline/Expire)
 * ne portent pas toujours le destinataire à notifier, on le résout ici à partir de l'état
 * accumulé (Created/Started). Alimenté par le même handler que l'ingestion (même groupe).
 */
@Builder(toBuilder = true)
public record NotificationRouting(
        NotificationRoutingSource sourceType,
        String sourceId,
        String initiatorId,
        String opponentId,
        String participantId,
        String topicId,
        Instant updatedAt
) {
}
