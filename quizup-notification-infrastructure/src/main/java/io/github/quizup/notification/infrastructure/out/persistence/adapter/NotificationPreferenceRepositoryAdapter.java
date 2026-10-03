package io.github.quizup.notification.infrastructure.out.persistence.adapter;

import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationPreference;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.infrastructure.out.persistence.mapper.NotificationPreferenceEntityMapper;
import io.github.quizup.notification.infrastructure.out.persistence.repository.NotificationPreferenceJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class NotificationPreferenceRepositoryAdapter implements NotificationPreferenceRepositoryPort {

    private final NotificationPreferenceJpaRepository repository;

    public NotificationPreferenceRepositoryAdapter(NotificationPreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(NotificationPreference preference) {
        repository.save(NotificationPreferenceEntityMapper.toEntity(preference));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationPreference> find(String userId, NotificationCategory category) {
        return repository.findByUserIdAndCategory(userId, category).stream()
                .findFirst()
                .map(NotificationPreferenceEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreference> findByUserId(String userId) {
        return repository.findByUserId(userId).stream()
                .map(NotificationPreferenceEntityMapper::toDomain)
                .toList();
    }
}
