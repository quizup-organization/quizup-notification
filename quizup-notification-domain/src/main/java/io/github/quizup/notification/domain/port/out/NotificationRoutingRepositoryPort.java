package io.github.quizup.notification.domain.port.out;

import io.github.quizup.notification.domain.model.NotificationRouting;
import io.github.quizup.notification.domain.model.NotificationRoutingSource;

import java.util.Optional;

/** Port sortant de l'index de routage des destinataires. */
public interface NotificationRoutingRepositoryPort {

    void save(NotificationRouting routing);

    Optional<NotificationRouting> find(NotificationRoutingSource sourceType, String sourceId);

    void delete(NotificationRoutingSource sourceType, String sourceId);
}
