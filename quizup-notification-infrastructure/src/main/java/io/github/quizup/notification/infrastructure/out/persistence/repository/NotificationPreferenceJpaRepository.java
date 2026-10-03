package io.github.quizup.notification.infrastructure.out.persistence.repository;

import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.infrastructure.out.persistence.entity.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationPreferenceJpaRepository
        extends JpaRepository<NotificationPreferenceEntity, NotificationPreferenceEntity.Key> {

    List<NotificationPreferenceEntity> findByUserId(String userId);

    List<NotificationPreferenceEntity> findByUserIdAndCategory(String userId, NotificationCategory category);
}
