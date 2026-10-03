package io.github.quizup.notification.infrastructure.out.persistence.entity;

import io.github.quizup.notification.domain.model.NotificationCategory;
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
@Table(name = "notification_preference_entry")
@IdClass(NotificationPreferenceEntity.Key.class)
public class NotificationPreferenceEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private NotificationCategory category;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Clé composite (user_id, category). */
    public static class Key implements Serializable {

        private String userId;
        private NotificationCategory category;

        public Key() {
        }

        public Key(String userId, NotificationCategory category) {
            this.userId = userId;
            this.category = category;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(userId, key.userId) && category == key.category;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, category);
        }
    }
}
