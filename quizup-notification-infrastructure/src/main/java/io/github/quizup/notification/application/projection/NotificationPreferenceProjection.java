package io.github.quizup.notification.application.projection;

import io.github.quizup.notification.domain.event.NotificationPreferenceUpdatedEvent;
import io.github.quizup.notification.domain.model.NotificationPreference;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Projection des préférences : seules les catégories désactivées/activées explicitement y sont. */
@Component
@ProcessingGroup("notification-preference-projection")
public class NotificationPreferenceProjection {

    private final NotificationPreferenceRepositoryPort repository;

    public NotificationPreferenceProjection(NotificationPreferenceRepositoryPort repository) {
        this.repository = repository;
    }

    @EventHandler
    @Transactional
    public void on(NotificationPreferenceUpdatedEvent event) {
        repository.save(new NotificationPreference(
                event.userId(),
                event.category(),
                event.enabled(),
                event.updatedAt()));
    }
}
