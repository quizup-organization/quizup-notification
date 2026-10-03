package io.github.quizup.notification.infrastructure.out.persistence.entity;

import io.github.quizup.notification.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "notification_entry", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id, created_at"),
        @Index(name = "idx_notification_unread", columnList = "user_id, read_at")
})
public class NotificationEntity {

    @Id
    @Column(name = "notification_id", nullable = false)
    private String notificationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "topic_id")
    private String topicId;

    @Column(name = "game_id")
    private String gameId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
