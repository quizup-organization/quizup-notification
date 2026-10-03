package io.github.quizup.notification.infrastructure.out.persistence.adapter;

import io.github.quizup.notification.domain.model.NotificationRouting;
import io.github.quizup.notification.domain.model.NotificationRoutingSource;
import io.github.quizup.notification.domain.port.out.NotificationRoutingRepositoryPort;
import io.github.quizup.notification.infrastructure.out.persistence.entity.NotificationRoutingEntity;
import io.github.quizup.notification.infrastructure.out.persistence.mapper.NotificationRoutingEntityMapper;
import io.github.quizup.notification.infrastructure.out.persistence.repository.NotificationRoutingJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class NotificationRoutingRepositoryAdapter implements NotificationRoutingRepositoryPort {

    private final NotificationRoutingJpaRepository repository;

    public NotificationRoutingRepositoryAdapter(NotificationRoutingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(NotificationRouting routing) {
        repository.save(NotificationRoutingEntityMapper.toEntity(routing));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationRouting> find(NotificationRoutingSource sourceType, String sourceId) {
        return repository.findById(new NotificationRoutingEntity.Key(sourceType, sourceId))
                .map(NotificationRoutingEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(NotificationRoutingSource sourceType, String sourceId) {
        repository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    }
}
