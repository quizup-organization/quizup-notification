package io.github.quizup.notification.application.handler.event;

import io.github.quizup.matchmaking.domain.event.ChallengeEvent;
import io.github.quizup.matchmaking.domain.event.RoomEvent;
import io.github.quizup.matchmaking.domain.model.ChallengeRoomId;
import io.github.quizup.notification.domain.command.NotificationCommand;
import io.github.quizup.notification.domain.model.NotificationCategory;
import io.github.quizup.notification.domain.model.NotificationPreference;
import io.github.quizup.notification.domain.model.NotificationType;
import io.github.quizup.notification.domain.port.out.NotificationPreferenceRepositoryPort;
import io.github.quizup.notification.domain.port.out.NotificationRepositoryPort;
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
 * L'identifiant de notification est déterministe (type + source + destinataire) : la relecture
 * at-least-once est idempotente (contrainte unique en base).
 * <p>
 * Les événements de salle ne portent plus de notification d'inbox : la présence et l'échec de
 * préparation sont poussés en temps réel par le BFF sur {@code /topic/rooms/{roomId}}. Seul
 * {@code ROOM_COMPLETED} complète la notification d'acceptation avec le {@code gameId} (deep link
 * arène, la salle étant purgée après rétention).
 */
@Component
@ProcessingGroup("notification-ingestion")
public class NotificationIngestionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationIngestionHandler.class);

    private final CommandGateway commandGateway;
    private final NotificationRepositoryPort notificationRepository;
    private final NotificationPreferenceRepositoryPort preferenceRepository;
    private final EventStore eventStore;

    public NotificationIngestionHandler(CommandGateway commandGateway,
                                        NotificationRepositoryPort notificationRepository,
                                        NotificationPreferenceRepositoryPort preferenceRepository,
                                        EventStore eventStore) {
        this.commandGateway = commandGateway;
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
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

    /**
     * Défi accepté : l'invitation n'est plus actionnable et le lanceur est notifié de
     * l'acceptation (sourceId = roomId déterministe) ; la salle n'est pas encore rejointe.
     */
    @EventHandler
    public void on(ChallengeEvent.ChallengeAcceptedEvent event) {
        notificationRepository.expireChallengeInvitations(event.challengeId(), event.acceptedAt());
        createIfAllowed(event.challengerId(), NotificationType.ROOM_ACCEPTED, event.opponentId(),
                ChallengeRoomId.of(event.challengeId()), event.topicId(), null, null);
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

    // =============================== Salle ==============================

    @EventHandler
    public void on(RoomEvent.RoomCompletedEvent event) {
        // Deep link : les notifications d'acceptation pointent vers la partie créée — l'inbox
        // peut rejoindre l'arène même après la purge de la salle (rétention 2 min).
        notificationRepository.attachGameId(event.roomId(), event.gameId());
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
