package io.github.quizup.notification.infrastructure.out.persistence.mapper;

import io.github.quizup.notification.domain.model.NotificationRouting;
import io.github.quizup.notification.infrastructure.out.persistence.entity.NotificationRoutingEntity;

public final class NotificationRoutingEntityMapper {

    private NotificationRoutingEntityMapper() {
    }

    public static NotificationRouting toDomain(NotificationRoutingEntity entity) {
        return NotificationRouting.builder()
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .initiatorId(entity.getInitiatorId())
                .opponentId(entity.getOpponentId())
                .participantId(entity.getParticipantId())
                .topicId(entity.getTopicId())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static NotificationRoutingEntity toEntity(NotificationRouting routing) {
        NotificationRoutingEntity entity = new NotificationRoutingEntity();
        entity.setSourceType(routing.sourceType());
        entity.setSourceId(routing.sourceId());
        entity.setInitiatorId(routing.initiatorId());
        entity.setOpponentId(routing.opponentId());
        entity.setParticipantId(routing.participantId());
        entity.setTopicId(routing.topicId());
        entity.setUpdatedAt(routing.updatedAt());
        return entity;
    }
}
