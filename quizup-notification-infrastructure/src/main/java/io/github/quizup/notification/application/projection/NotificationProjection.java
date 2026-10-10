package io.github.quizup.notification.application.projection;

import io.github.quizup.notification.domain.event.NotificationEvent;
import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projection du read model « inbox ». La date de lecture évolue, et la suppression est un
 * hard delete de la ligne (l'agrégat, lui, est marqué supprimé dans l'event store).
 */
@Component
@ProcessingGroup("notification-projection")
public class NotificationProjection {

    private final NotificationRepositoryPort repository;

    public NotificationProjection(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @EventHandler
    @Transactional
    public void on(NotificationEvent.NotificationCreatedEvent event) {
        repository.save(Notification.builder()
                .notificationId(event.notificationId())
                .userId(event.userId())
                .type(event.type())
                .actorId(event.actorId())
                .sourceId(event.sourceId())
                .topicId(event.topicId())
                .gameId(event.gameId())
                .expiresAt(event.expiresAt())
                .readAt(null)
                .createdAt(event.createdAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(NotificationEvent.NotificationReadEvent event) {
        repository.findById(event.notificationId()).ifPresent(notification ->
                repository.save(notification.toBuilder()
                        .readAt(event.readAt())
                        .build()));
    }

    @EventHandler
    @Transactional
    public void on(NotificationEvent.NotificationUnreadEvent event) {
        repository.findById(event.notificationId()).ifPresent(notification ->
                repository.save(notification.toBuilder()
                        .readAt(null)
                        .build()));
    }

    @EventHandler
    @Transactional
    public void on(NotificationEvent.NotificationDeletedEvent event) {
        repository.deleteById(event.notificationId());
    }
}
