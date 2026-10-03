package io.github.quizup.notification.infrastructure.out.persistence.repository;

import io.github.quizup.notification.infrastructure.out.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(String userId);

    List<NotificationEntity> findByUserIdAndReadAtIsNull(String userId);
}
