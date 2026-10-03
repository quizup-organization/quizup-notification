package io.github.quizup.notification.domain.query;

import io.github.quizup.notification.domain.model.NotificationPage;
import io.github.quizup.notification.domain.model.NotificationPreference;

import java.util.List;

/** Queries du domaine notification (vues BFF). */
public interface NotificationQuery {

    record GetNotificationsQuery(String userId, boolean unreadOnly, int page, int size)
            implements NotificationQuery {
    }

    record GetNotificationQuery(String userId, String notificationId) implements NotificationQuery {
    }

    record CountUnreadNotificationsQuery(String userId) implements NotificationQuery {
    }

    record GetNotificationPreferencesQuery(String userId) implements NotificationQuery {
    }

    record GetUnreadNotificationIdsQuery(String userId) implements NotificationQuery {
    }
}
