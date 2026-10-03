package io.github.quizup.notification.application.handler.event;

import io.github.quizup.matchmaking.domain.event.LobbyEvent;
import io.github.quizup.matchmaking.domain.event.MatchmakingEvent;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationPreference;
import io.github.quizup.notification.domain.model.NotificationRouting;
import io.github.quizup.notification.domain.model.NotificationRoutingSource;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRoutingRepositoryPort;
import io.github.quizup.social.domain.event.UserFollowerEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Ingestion des événements de domaine (Kafka) → notifications personnelles.
 * <p>
 * Le routage des destinataires et la création sont dans le **même processing group** : l'ordre
 * par agrégat (clé Kafka) garantit que l'index de routage est à jour avant les événements
 * terminaux. L'identifiant de notification est déterministe (type + source + destinataire) :
 * la relecture at-least-once est idempotente (contrainte unique en base).
 */
@Component
@ProcessingGroup("notification-ingestion")
public class NotificationIngestionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationIngestionHandler.class);

    private static final String PLAYER_LEFT = "PLAYER_LEFT";

    private final CommandGateway commandGateway;
    private final NotificationRepositoryPort notificationRepository;
    private final NotificationPreferenceRepositoryPort preferenceRepository;
    private final NotificationRoutingRepositoryPort routingRepository;

    public NotificationIngestionHandler(CommandGateway commandGateway,
                                        NotificationRepositoryPort notificationRepository,
                                        NotificationPreferenceRepositoryPort preferenceRepository,
                                        NotificationRoutingRepositoryPort routingRepository) {
        this.commandGateway = commandGateway;
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.routingRepository = routingRepository;
    }

    // ============================== Follows ==============================

    @EventHandler
    public void on(UserFollowerEvent.UserFollowedEvent event) {
        createIfAllowed(event.followedId(), NotificationType.FOLLOW, event.followerId(),
                event.followId(), null, null, null);
    }

    // =============================== Salons ==============================

    @EventHandler
    public void on(LobbyEvent.LobbyCreatedEvent event) {
        routingRepository.save(NotificationRouting.builder()
                .sourceType(NotificationRoutingSource.LOBBY)
                .sourceId(event.lobbyId())
                .initiatorId(event.initiatorId())
                .opponentId(event.opponentId())
                .topicId(event.topicId())
                .updatedAt(event.createdAt())
                .build());
        if (event.opponentId() != null) {
            createIfAllowed(event.opponentId(), NotificationType.LOBBY_INVITATION, event.initiatorId(),
                    event.lobbyId(), event.topicId(), null, event.expiresAt());
        }
    }

    @EventHandler
    public void on(LobbyEvent.LobbyJoinedEvent event) {
        routingRepository.find(NotificationRoutingSource.LOBBY, event.lobbyId()).ifPresent(routing -> {
            routingRepository.save(routing.toBuilder()
                    .participantId(event.participantId())
                    .updatedAt(event.joinedAt())
                    .build());
            createIfAllowed(routing.initiatorId(), NotificationType.LOBBY_ACCEPTED, event.participantId(),
                    event.lobbyId(), routing.topicId(), null, null);
        });
    }

    @EventHandler
    public void on(LobbyEvent.LobbyDeclinedEvent event) {
        String topicId = routingRepository
                .find(NotificationRoutingSource.LOBBY, event.lobbyId())
                .map(NotificationRouting::topicId)
                .orElse(null);
        createIfAllowed(event.initiatorId(), NotificationType.LOBBY_DECLINED, event.opponentId(),
                event.lobbyId(), topicId, null, null);
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
    }

    @EventHandler
    public void on(LobbyEvent.LobbyCancelledEvent event) {
        routingRepository.find(NotificationRoutingSource.LOBBY, event.lobbyId()).ifPresent(routing -> {
            String recipient = PLAYER_LEFT.equals(event.reason())
                    ? routing.initiatorId()
                    : Optional.ofNullable(routing.participantId()).orElse(routing.opponentId());
            createIfAllowed(recipient, NotificationType.LOBBY_CANCELLED, event.initiatorId(),
                    event.lobbyId(), routing.topicId(), null, null);
            routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
        });
    }

    @EventHandler
    public void on(LobbyEvent.LobbyExpiredEvent event) {
        routingRepository.find(NotificationRoutingSource.LOBBY, event.lobbyId()).ifPresent(routing -> {
            createIfAllowed(routing.initiatorId(), NotificationType.LOBBY_EXPIRED, routing.opponentId(),
                    event.lobbyId(), routing.topicId(), null, null);
            // Défi nominatif : l'invité doit aussi savoir que l'invitation n'est plus valable
            // (sinon sa notification d'invitation reste actionnable jusqu'à la purge → 404).
            if (routing.opponentId() != null) {
                createIfAllowed(routing.opponentId(), NotificationType.LOBBY_EXPIRED, routing.initiatorId(),
                        event.lobbyId(), routing.topicId(), null, null);
            }
            routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
        });
    }

    @EventHandler
    public void on(LobbyEvent.LobbyCompletedEvent event) {
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
    }

    @EventHandler
    public void on(LobbyEvent.LobbyPurgedEvent event) {
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
    }

    // ============================ Appariement ============================

    @EventHandler
    public void on(MatchmakingEvent.MatchmakingStartedEvent event) {
        routingRepository.save(NotificationRouting.builder()
                .sourceType(NotificationRoutingSource.MATCHMAKING)
                .sourceId(event.matchmakingId())
                .initiatorId(event.playerId())
                .topicId(event.topicId())
                .updatedAt(event.startedAt())
                .build());
    }

    @EventHandler
    public void on(MatchmakingEvent.MatchmakingMatchedEvent event) {
        routingRepository.find(NotificationRoutingSource.MATCHMAKING, event.matchmakingId())
                .ifPresent(routing -> {
                    createIfAllowed(routing.initiatorId(), NotificationType.MATCHMAKING_READY,
                            event.opponentId(), event.matchmakingId(), routing.topicId(), event.gameId(), null);
                    routingRepository.delete(NotificationRoutingSource.MATCHMAKING, event.matchmakingId());
                });
    }

    @EventHandler
    public void on(MatchmakingEvent.MatchmakingCancelledEvent event) {
        routingRepository.delete(NotificationRoutingSource.MATCHMAKING, event.matchmakingId());
    }

    @EventHandler
    public void on(MatchmakingEvent.MatchmakingFailedEvent event) {
        routingRepository.delete(NotificationRoutingSource.MATCHMAKING, event.matchmakingId());
    }

    @EventHandler
    public void on(MatchmakingEvent.MatchmakingPurgedEvent event) {
        routingRepository.delete(NotificationRoutingSource.MATCHMAKING, event.matchmakingId());
    }

    // =====================================================================

    private void createIfAllowed(String userId,
                                 NotificationType type,
                                 String actorId,
                                 String sourceId,
                                 String topicId,
                                 String gameId,
                                 Instant expiresAt) {
        if (userId == null || sourceId == null) {
            return;
        }
        if (!isEnabled(userId, type.category())) {
            logger.debug("Notification {} ignorée (préférence {} désactivée) pour {}",
                    type, type.category(), userId);
            return;
        }
        String notificationId = deterministicId(type, sourceId, userId);
        if (notificationRepository.existsById(notificationId)) {
            return;
        }
        commandGateway.send(new NotificationCommand.CreateNotificationCommand(
                notificationId, userId, type, actorId, sourceId, topicId, gameId, expiresAt));
    }

    private boolean isEnabled(String userId, NotificationCategory category) {
        return preferenceRepository.find(userId, category)
                .map(NotificationPreference::enabled)
                .orElse(true);
    }

    private static String deterministicId(NotificationType type, String sourceId, String userId) {
        return UUID.nameUUIDFromBytes(
                        (type + ":" + sourceId + ":" + userId).getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}
