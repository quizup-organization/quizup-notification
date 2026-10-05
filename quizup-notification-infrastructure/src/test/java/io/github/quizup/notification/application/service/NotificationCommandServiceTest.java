package io.github.quizup.notification.application.service;

import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationCommandServiceTest {

    private final NotificationRepositoryPort notificationRepository = mock(NotificationRepositoryPort.class);
    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final NotificationCommandService service =
            new NotificationCommandService(notificationRepository, commandGateway);

    @Test
    void deleteAll_fans_out_delete_commands() {
        when(notificationRepository.findAllIds("user-1")).thenReturn(List.of("n1", "n2"));

        service.handle(new NotificationCommand.DeleteAllNotificationsCommand("user-1"));

        verify(commandGateway).send(new NotificationCommand.DeleteNotificationCommand("n1", "user-1"));
        verify(commandGateway).send(new NotificationCommand.DeleteNotificationCommand("n2", "user-1"));
    }

    @Test
    void deleteAll_without_notification_sends_nothing() {
        when(notificationRepository.findAllIds("user-1")).thenReturn(List.of());

        service.handle(new NotificationCommand.DeleteAllNotificationsCommand("user-1"));

        verifyNoInteractions(commandGateway);
    }
}
