package io.github.quizup.notification.application.handler.event;

import io.github.quizup.matchmaking.domain.event.ChallengeEvent;
import io.github.quizup.matchmaking.domain.event.LobbyEvent;
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
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.modelling.command.AggregateStreamCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Ingestion des événements de domaine (Kafka) → notifications personnelles.
 * <p>
 * Le routage des destinataires et la création sont dans le **même processing group** : l'ordre
 * par agrégat (clé Kafka) garantit que l'index de routage est à jour avant les événements
 * terminaux. L'identifiant de notification est déterministe (type + source + destinataire) :
 * la relecture at-least-once est idempotente (contrainte unique en base).
 * <p>
 * À la clôture d'un salon, l'**invitation en attente est expirée** dans le read model
 * (`expiresAt` = instant de l'événement) : le client masque Accepter/Refuser et l'acceptation
 * tardive (salon purgé → 404) est évitée.
 */
@Component
@ProcessingGroup("notification-ingestion")
public class NotificationIngestionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationIngestionHandler.class);

    private final CommandGateway commandGateway;
    private final NotificationRepositoryPort notificationRepository;
    private final NotificationPreferenceRepositoryPort preferenceRepository;
    private final NotificationRoutingRepositoryPort routingRepository;
    private final EventStore eventStore;

    public NotificationIngestionHandler(CommandGateway commandGateway,
                                        NotificationRepositoryPort notificationRepository,
                                        NotificationPreferenceRepositoryPort preferenceRepository,
                                        NotificationRoutingRepositoryPort routingRepository,
                                        EventStore eventStore) {
        this.commandGateway = commandGateway;
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.routingRepository = routingRepository;
        this.eventStore = eventStore;
    }

    // ============================== Follows ==============================

    @EventHandler
    public void on(UserFollowerEvent.UserFollowedEvent event) {
        createIfAllowed(event.followedId(), NotificationType.FOLLOW, event.followerId(),
                event.followId(), null, null, null);
    }

    // ============================ Défis nominatifs ============================

    /** Défi reçu : l'invitation vit au niveau du défi (sourceId = challengeId), avant la salle. */
    @EventHandler
    public void on(ChallengeEvent.ChallengeCreatedEvent event) {
        createIfAllowed(event.opponentId(), NotificationType.CHALLENGE_RECEIVED, event.challengerId(),
                event.challengeId(), event.topicId(), null, event.expiresAt());
    }

    @EventHandler
    public void on(ChallengeEvent.ChallengeAcceptedEvent event) {
        // Le défi est accepté : la salle est créée par la saga ; l'invitation n'est plus actionnable.
        notificationRepository.expireChallengeInvitations(event.challengeId(), event.acceptedAt());
    }

    @EventHandler
    public void on(ChallengeEvent.ChallengeDeclinedEvent event) {
        notificationRepository.expireChallengeInvitations(event.challengeId(), event.declinedAt());
        createIfAllowed(event.challengerId(), NotificationType.CHALLENGE_DECLINED, event.opponentId(),
                event.challengeId(), null, null, null);
    }

    @EventHandler
    public void on(ChallengeEvent.ChallengeCancelledEvent event) {
        notificationRepository.expireChallengeInvitations(event.challengeId(), event.cancelledAt());
    }

    @EventHandler
    public void on(ChallengeEvent.ChallengeExpiredEvent event) {
        notificationRepository.expireChallengeInvitations(event.challengeId(), event.expiredAt());
    }

    @EventHandler
    public void on(ChallengeEvent.ChallengePurgedEvent event) {
        // Filet de sécurité (la purge suit normalement un événement terminal déjà traité) :
        // l'invitation en attente n'est plus actionnable une fois le défi disparu.
        notificationRepository.expireChallengeInvitations(event.challengeId(), event.purgedAt());
    }

    // =============================== Salons ==============================

    @EventHandler
    public void on(LobbyEvent.LobbyCreatedEvent event) {
        // Index de routage uniquement : l'invitation nominative vit désormais dans le défi
        // (les salles nominatives sont créées par la saga à l'acceptation).
        routingRepository.save(NotificationRouting.builder()
                .sourceType(NotificationRoutingSource.LOBBY)
                .sourceId(event.lobbyId())
                .initiatorId(event.initiatorId())
                .opponentId(event.opponentId())
                .topicId(event.topicId())
                .updatedAt(event.createdAt())
                .build());
    }

    @EventHandler
    public void on(LobbyEvent.LobbyJoinedEvent event) {
        // L'invitation n'est plus en attente dès que le salon est rejoint (même depuis
        // un autre onglet/appareil) : on l'expire pour masquer Accepter/Refuser.
        notificationRepository.expireInvitations(event.lobbyId(), event.joinedAt());
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
        notificationRepository.expireInvitations(event.lobbyId(), event.declinedAt());
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
        // Plus de notification « défi annulé » : l'invitation en attente est simplement expirée.
        notificationRepository.expireInvitations(event.lobbyId(), event.cancelledAt());
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
    }

    @EventHandler
    public void on(LobbyEvent.LobbyExpiredEvent event) {
        // Plus de notification « défi expiré » : l'invitation en attente est simplement expirée.
        notificationRepository.expireInvitations(event.lobbyId(), event.expiredAt());
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
    }

    @EventHandler
    public void on(LobbyEvent.LobbyCompletedEvent event) {
        notificationRepository.expireInvitations(event.lobbyId(), event.completedAt());
        // Deep link : les notifications d'acceptation déjà émises pointent vers la partie créée
        // (le salon est purgé 2 min après ; le gameId reste dans l'inbox).
        notificationRepository.attachGameId(event.lobbyId(), event.gameId());
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
    }

    /** Échec système : la partie n'a pas pu être créée, l'invitation devient non actionnable. */
    @EventHandler
    public void on(LobbyEvent.LobbyFailedEvent event) {
        notificationRepository.expireInvitations(event.lobbyId(), event.failedAt());
    }

    /**
     * Un joueur ne s'est pas présenté en salle : trace durable pour l'autre (le joueur qui a
     * attendu). L'acteur de la notification est l'absent.
     */
    @EventHandler
    public void on(LobbyEvent.LobbyMissedEvent event) {
        notificationRepository.expireInvitations(event.lobbyId(), event.missedAt());
        routingRepository.find(NotificationRoutingSource.LOBBY, event.lobbyId()).ifPresent(routing -> {
            String recipient = event.absentPlayerId() != null
                    && event.absentPlayerId().equals(routing.initiatorId())
                    ? routing.participantId()
                    : routing.initiatorId();
            if (recipient != null) {
                createIfAllowed(recipient, NotificationType.LOBBY_MISSED, event.absentPlayerId(),
                        event.lobbyId(), routing.topicId(), null, null);
            }
        });
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
    }

    @EventHandler
    public void on(LobbyEvent.LobbyPurgedEvent event) {
        // Filet de sécurité (la purge suit normalement un événement terminal déjà traité).
        notificationRepository.expireInvitations(event.lobbyId(), event.purgedAt());
        routingRepository.delete(NotificationRoutingSource.LOBBY, event.lobbyId());
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
        // Le read model seul ne suffit pas : après une suppression (hard delete), une relecture
        // at-least-once (replay Kafka) ne voit plus la ligne et retenterait de créer un agrégat
        // existant → poison pill. L'event store est la garde d'idempotence durable.
        if (notificationRepository.existsById(notificationId) || aggregateExists(notificationId)) {
            return;
        }
        try {
            commandGateway.send(new NotificationCommand.CreateNotificationCommand(
                    notificationId, userId, type, actorId, sourceId, topicId, gameId, expiresAt));
        } catch (RuntimeException error) {
            if (isAggregateAlreadyCreated(error)) {
                logger.warn("Notification {} déjà créée (agrégat existant) — relecture ignorée",
                        notificationId);
                return;
            }
            throw error;
        }
    }

    private boolean aggregateExists(String notificationId) {
        return eventStore.readEvents(notificationId).hasNext();
    }

    private static boolean isAggregateAlreadyCreated(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof AggregateStreamCreationException) {
                return true;
            }
        }
        return false;
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
