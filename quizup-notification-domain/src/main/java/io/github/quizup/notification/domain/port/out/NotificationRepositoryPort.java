package io.github.quizup.notification.domain.port.out;

import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.domain.model.NotificationPage;

import java.util.List;
import java.util.Optional;

/** Port sortant du read model « inbox ». */
public interface NotificationRepositoryPort {

    void save(Notification notification);

    /** Hard delete de la notification (projection de l'événement de suppression). */
    void deleteById(String notificationId);

    /**
     * Expire les invitations de défi en attente d'un salon (salon clos : accepté, refusé,
     * annulé, expiré, échec ou purgé). Le client masque alors les actions d'acceptation.
     */
    void expireInvitations(String sourceId, java.time.Instant expiredAt);

    Optional<Notification> findById(String notificationId);

    boolean existsById(String notificationId);

    NotificationPage page(String userId, boolean unreadOnly, int page, int size);

    long countUnread(String userId);

    List<String> findUnreadIds(String userId);
}
