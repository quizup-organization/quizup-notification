package io.github.quizup.notification.application.service;

import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Component;

/**
 * Handler de commande sans état : « tout marquer lu » se traduit par un fan-out de commandes
 * unitaires (idempotentes, chacune vérifiant le propriétaire).
 */
@Component
public class NotificationCommandService {

    private final NotificationRepositoryPort notificationRepository;
    private final CommandGateway commandGateway;

    public NotificationCommandService(NotificationRepositoryPort notificationRepository,
                                      CommandGateway commandGateway) {
        this.notificationRepository = notificationRepository;
        this.commandGateway = commandGateway;
    }

    @CommandHandler
    public void handle(NotificationCommand.MarkAllNotificationsReadCommand command) {
        notificationRepository.findUnreadIds(command.userId()).forEach(notificationId ->
                commandGateway.send(new NotificationCommand.MarkNotificationReadCommand(
                        notificationId, command.userId())));
    }
}
