package io.github.quizup.notification.domain.aggregate;

import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.event.NotificationPreferenceUpdatedEvent;
import io.github.quizup.notification.domain.model.NotificationCategory;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * NotificationPreferenceAggregate — préférences d'un joueur (une catégorie peut être coupée).
 * Défaut : tout est activé ; seules les catégories désactivées sont mémorisées.
 */
@Aggregate
public class NotificationPreferenceAggregate {

    @AggregateIdentifier
    private String userId;
    private final Set<NotificationCategory> disabled = new HashSet<>();

    protected NotificationPreferenceAggregate() {
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(NotificationCommand.UpdateNotificationPreferenceCommand command) {
        apply(new NotificationPreferenceUpdatedEvent(
                command.userId(),
                command.category(),
                command.enabled(),
                Instant.now()));
    }

    // ============================ Event Sourcing ============================

    @EventSourcingHandler
    public void on(NotificationPreferenceUpdatedEvent event) {
        this.userId = event.userId();
        if (event.enabled()) {
            disabled.remove(event.category());
        } else {
            disabled.add(event.category());
        }
    }
}
