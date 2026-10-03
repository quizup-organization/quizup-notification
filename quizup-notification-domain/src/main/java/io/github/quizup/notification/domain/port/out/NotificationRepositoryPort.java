package io.github.quizup.notification.domain.port.out;

import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.domain.model.NotificationPage;

import java.util.List;
import java.util.Optional;

/** Port sortant du read model « inbox ». */
public interface NotificationRepositoryPort {

    void save(Notification notification);

    Optional<Notification> findById(String notificationId);

    boolean existsById(String notificationId);

    NotificationPage page(String userId, boolean unreadOnly, int page, int size);

    long countUnread(String userId);

    List<String> findUnreadIds(String userId);
}
