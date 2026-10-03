package io.github.quizup.notification.infrastructure.out.persistence.entity;

import io.github.quizup.notification.domain.model.NotificationRoutingSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Setter
@Getter
@Entity
@Table(name = "notification_routing_entry")
@IdClass(NotificationRoutingEntity.Key.class)
public class NotificationRoutingEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private NotificationRoutingSource sourceType;

    @Id
    @Column(name = "source_id", nullable = false)
    private String sourceId;

    @Column(name = "initiator_id")
    private String initiatorId;

    @Column(name = "opponent_id")
    private String opponentId;

    @Column(name = "participant_id")
    private String participantId;

    @Column(name = "topic_id")
    private String topicId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Clé composite (source_type, source_id). */
    public static class Key implements Serializable {

        private NotificationRoutingSource sourceType;
        private String sourceId;

        public Key() {
        }

        public Key(NotificationRoutingSource sourceType, String sourceId) {
            this.sourceType = sourceType;
            this.sourceId = sourceId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return sourceType == key.sourceType && Objects.equals(sourceId, key.sourceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceType, sourceId);
        }
    }
}
