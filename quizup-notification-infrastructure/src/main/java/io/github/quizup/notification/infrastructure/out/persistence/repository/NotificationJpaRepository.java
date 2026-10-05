package io.github.quizup.notification.infrastructure.out.persistence.repository;

import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.infrastructure.out.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(String userId);

    List<NotificationEntity> findByUserIdAndReadAtIsNull(String userId);

    /** Expire les notifications d'une source (invitations d'un salon clos) sans toucher au reste. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.expiresAt = :expiredAt "
            + "where n.sourceId = :sourceId and n.type = :type")
    int expireBySourceIdAndType(@Param("sourceId") String sourceId,
                                @Param("type") NotificationType type,
                                @Param("expiredAt") Instant expiredAt);

    /** Rattache la partie créée aux notifications d'acceptation de la source (deep link arène). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationEntity n set n.gameId = :gameId "
            + "where n.sourceId = :sourceId and n.type = :type")
    int attachGameId(@Param("sourceId") String sourceId,
                     @Param("type") NotificationType type,
                     @Param("gameId") String gameId);
}
