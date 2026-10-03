package io.github.quizup.notification.domain.exception;

import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.util.Map;

public final class NotificationExceptions {

    private NotificationExceptions() {
    }

    public static class MissingRecipientProblem extends NotificationProblem {
        public MissingRecipientProblem(String notificationId) {
            super("urn:quizup:notification:missingRecipient",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Destinataire manquant",
                    "Une notification doit avoir un destinataire",
                    Map.of("notificationId", notificationId));
        }
    }

    public static class NotNotificationOwnerProblem extends NotificationProblem {
        public NotNotificationOwnerProblem(String notificationId, String userId) {
            super("urn:quizup:notification:notOwner",
                    ProblemCategory.PERMISSION,
                    "Notification d'un autre joueur",
                    "Le joueur " + userId + " n'est pas le destinataire de cette notification",
                    Map.of("notificationId", notificationId));
        }
    }

    public static class NotificationNotFoundProblem extends NotificationProblem {
        public NotificationNotFoundProblem(String notificationId) {
            super("urn:quizup:notification:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Notification introuvable",
                    "La notification " + notificationId + " n'existe pas",
                    Map.of("notificationId", notificationId));
        }
    }
}
