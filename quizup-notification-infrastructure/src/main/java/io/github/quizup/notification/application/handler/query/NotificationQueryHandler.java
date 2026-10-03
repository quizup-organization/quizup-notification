package io.github.quizup.notification.application.handler.query;

import io.github.quizup.notification.domain.exception.NotificationExceptions;
import io.github.quizup.notification.domain.model.Notification;
import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationPage;
import io.github.quizup.notification.domain.model.NotificationPreference;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.notification.domain.query.NotificationQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationQueryHandler {

    private final NotificationRepositoryPort notificationRepository;
    private final NotificationPreferenceRepositoryPort preferenceRepository;

    public NotificationQueryHandler(NotificationRepositoryPort notificationRepository,
                                    NotificationPreferenceRepositoryPort preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @QueryHandler
    public NotificationPage handle(NotificationQuery.GetNotificationsQuery query) {
        return notificationRepository.page(
                query.userId(), query.unreadOnly(), query.page(), query.size());
    }

    @QueryHandler
    public Notification handle(NotificationQuery.GetNotificationQuery query) {
        Notification notification = notificationRepository.findById(query.notificationId())
                .orElseThrow(() -> new NotificationExceptions.NotificationNotFoundProblem(query.notificationId()));
        if (!notification.userId().equals(query.userId())) {
            throw new NotificationExceptions.NotNotificationOwnerProblem(query.notificationId(), query.userId());
        }
        return notification;
    }

    @QueryHandler
    public Long handle(NotificationQuery.CountUnreadNotificationsQuery query) {
        return notificationRepository.countUnread(query.userId());
    }

    /** Toutes les catégories sont renvoyées : défaut activé si aucune préférence enregistrée. */
    @QueryHandler
    public List<NotificationPreference> handle(NotificationQuery.GetNotificationPreferencesQuery query) {
        Map<NotificationCategory, NotificationPreference> stored = preferenceRepository
                .findByUserId(query.userId()).stream()
                .collect(Collectors.toMap(NotificationPreference::category, Function.identity()));
        List<NotificationPreference> preferences = new ArrayList<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            NotificationPreference preference = stored.get(category);
            preferences.add(preference != null
                    ? preference
                    : new NotificationPreference(query.userId(), category, true, Instant.EPOCH));
        }
        return preferences;
    }

    @QueryHandler
    public List<String> handle(NotificationQuery.GetUnreadNotificationIdsQuery query) {
        return notificationRepository.findUnreadIds(query.userId());
    }
}
