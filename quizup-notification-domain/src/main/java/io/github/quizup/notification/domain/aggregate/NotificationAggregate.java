package io.github.quizup.notification.domain.aggregate;

import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.event.NotificationEvent;
import io.github.quizup.notification.domain.exception.NotificationExceptions;
import io.github.quizup.notification.domain.model.NotificationType;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * NotificationAggregate — une notification personnelle (inbox).
 * <p>
 * Créée par l'ingestion d'un événement de domaine (identifiant déterministe : la relecture
 * Kafka est idempotente). Le destinataire peut la marquer lue ou la supprimer ; personne d'autre.
 * La suppression est un hard delete : l'agrégat est marqué supprimé et la projection purge la ligne.
 */
@Aggregate
public class NotificationAggregate {

    private static final Logger logger = LoggerFactory.getLogger(NotificationAggregate.class);

    @AggregateIdentifier
    private String notificationId;
    private String userId;
    private NotificationType type;
    private Instant readAt;

    protected NotificationAggregate() {
    }

    @CommandHandler
    public NotificationAggregate(NotificationCommand.CreateNotificationCommand command) {
        if (StringUtils.isBlank(command.userId())) {
            throw new NotificationExceptions.MissingRecipientProblem(command.notificationId());
        }
        logger.debug("Creating notification: id={}, userId={}, type={}",
                command.notificationId(), command.userId(), command.type());
        apply(new NotificationEvent.NotificationCreatedEvent(
                command.notificationId(),
                command.userId(),
                command.type(),
                command.actorId(),
                command.sourceId(),
                command.topicId(),
                command.gameId(),
                command.expiresAt(),
                Instant.now()));
    }

    @CommandHandler
    public void handle(NotificationCommand.MarkNotificationReadCommand command) {
        if (!command.userId().equals(userId)) {
            throw new NotificationExceptions.NotNotificationOwnerProblem(notificationId, command.userId());
        }
        if (readAt != null) {
            return;
        }
        apply(new NotificationEvent.NotificationReadEvent(notificationId, Instant.now()));
    }

    /** Seul le destinataire peut supprimer sa notification (hard delete, standard Axon). */
    @CommandHandler
    public void handle(NotificationCommand.DeleteNotificationCommand command) {
        if (!command.userId().equals(userId)) {
            throw new NotificationExceptions.NotNotificationOwnerProblem(notificationId, command.userId());
        }
        logger.debug("Deleting notification: id={}, userId={}", notificationId, command.userId());
        apply(new NotificationEvent.NotificationDeletedEvent(notificationId, userId, Instant.now()));
    }

    // ============================ Event Sourcing ============================

    @EventSourcingHandler
    public void on(NotificationEvent.NotificationCreatedEvent event) {
        this.notificationId = event.notificationId();
        this.userId = event.userId();
        this.type = event.type();
    }

    @EventSourcingHandler
    public void on(NotificationEvent.NotificationReadEvent event) {
        this.readAt = event.readAt();
    }

    @EventSourcingHandler
    public void on(NotificationEvent.NotificationDeletedEvent event) {
        AggregateLifecycle.markDeleted();
    }
}
