package io.github.quizup.notification.infrastructure.out.persistence.adapter;

import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.domain.model.NotificationPage;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.notification.infrastructure.out.persistence.mapper.NotificationEntityMapper;
import io.github.quizup.notification.infrastructure.out.persistence.repository.NotificationJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository repository;

    public NotificationRepositoryAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(Notification notification) {
        repository.save(NotificationEntityMapper.toEntity(notification));
    }

    @Override
    @Transactional
    public void deleteById(String notificationId) {
        repository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void expireInvitations(String sourceId, Instant expiredAt) {
        repository.expireBySourceIdAndType(sourceId, NotificationType.LOBBY_INVITATION, expiredAt);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findById(String notificationId) {
        return repository.findById(notificationId).map(NotificationEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String notificationId) {
        return repository.existsById(notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPage page(String userId, boolean unreadOnly, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = unreadOnly
                ? repository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageRequest)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        List<Notification> content = result.getContent().stream()
                .map(NotificationEntityMapper::toDomain)
                .toList();
        return new NotificationPage(content, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUnreadIds(String userId) {
        return repository.findByUserIdAndReadAtIsNull(userId).stream()
                .map(entity -> entity.getNotificationId())
                .toList();
    }
}
