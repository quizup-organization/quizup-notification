package io.github.quizup.notification.infrastructure.out.persistence.repository;

import io.github.quizup.notification.domain.model.NotificationRoutingSource;
import io.github.quizup.notification.infrastructure.out.persistence.entity.NotificationRoutingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRoutingJpaRepository
        extends JpaRepository<NotificationRoutingEntity, NotificationRoutingEntity.Key> {

    void deleteBySourceTypeAndSourceId(NotificationRoutingSource sourceType, String sourceId);
}
