package io.github.quizup.notification.domain.model;

import java.util.List;

/** Page de notifications (vue BFF). */
public record NotificationPage(List<Notification> content, long totalElements) {
}
