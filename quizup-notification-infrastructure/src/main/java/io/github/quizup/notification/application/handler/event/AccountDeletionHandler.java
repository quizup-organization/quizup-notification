package io.github.quizup.notification.application.handler.event;

import io.github.quizup.axon.autoconfigure.erasure.AxonEventStoreErasureService;
import io.github.quizup.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Supprime l'inbox et les préférences d'un compte supprimé, ainsi que les notifications dont il était l'acteur. */
@Component
@ProcessingGroup("account-deletion-notification")
public class AccountDeletionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final AxonEventStoreErasureService eventStoreErasureService;

    public AccountDeletionHandler(JdbcTemplate jdbcTemplate,
                                  AxonEventStoreErasureService eventStoreErasureService) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventStoreErasureService = eventStoreErasureService;
    }

    @EventHandler
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        String userId = event.userId();
        java.util.List<String> notificationIds = jdbcTemplate.queryForList(
                "SELECT notification_id FROM notification_entry WHERE user_id = ?",
                String.class, userId);
        jdbcTemplate.update(
                "DELETE FROM notification_entry WHERE user_id = ? OR actor_id = ?",
                userId, userId);
        jdbcTemplate.update(
                "DELETE FROM notification_preference_entry WHERE user_id = ?", userId);
        java.util.List<String> erasedAggregates = new java.util.ArrayList<>(notificationIds);
        erasedAggregates.add(userId); // agrégat de préférences : identifiant = userId
        eventStoreErasureService.eraseAggregates(erasedAggregates);
    }
}
