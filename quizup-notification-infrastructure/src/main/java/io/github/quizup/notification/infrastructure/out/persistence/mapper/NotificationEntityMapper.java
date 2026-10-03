package io.github.quizup.notification.infrastructure.out.persistence.mapper;

import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.infrastructure.out.persistence.entity.NotificationEntity;

public final class NotificationEntityMapper {

    private NotificationEntityMapper() {
    }

    public static Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .notificationId(entity.getNotificationId())
                .userId(entity.getUserId())
                .type(entity.getType())
                .actorId(entity.getActorId())
                .sourceId(entity.getSourceId())
                .topicId(entity.getTopicId())
                .gameId(entity.getGameId())
                .expiresAt(entity.getExpiresAt())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static NotificationEntity toEntity(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(notification.notificationId());
        entity.setUserId(notification.userId());
        entity.setType(notification.type());
        entity.setActorId(notification.actorId());
        entity.setSourceId(notification.sourceId());
        entity.setTopicId(notification.topicId());
        entity.setGameId(notification.gameId());
        entity.setExpiresAt(notification.expiresAt());
        entity.setReadAt(notification.readAt());
        entity.setCreatedAt(notification.createdAt());
        return entity;
    }
}
