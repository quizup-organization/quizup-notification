# AGENTS.md — quizup-notification

> Service de **notification** : inbox par joueur (lu/non-lu), préférences et ingestion des
> événements de domaine (follows, salons, appariement). Architecture : Axon Framework
> (CQRS/EDA) + JPA (projections) + Kafka (événements inter-services).
> Pour les règles de patterns : [
`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

- **Inbox** : consomme les événements de domaine des autres services et crée une notification
  personnelle par destinataire (`notification_entry` : type, acteur, source, sujet, partie,
  expiration, lu/non-lu). Le destinataire peut la marquer lue ou la **supprimer** (hard delete du
  read model, agrégat marqué supprimé).
- **Préférences** : une catégorie (`FOLLOW`, `LOBBY`) peut être coupée ; défaut activé. Une
  notification dont la catégorie est coupée n'est pas créée.
- **Livraison** : le service publie `NotificationCreatedEvent` sur le bus ; le **BFF** le pousse
  sur `/topic/notifications/{userId}` et l'expose en REST (`/api/notifications`,
  `/api/notification-preferences`).

**Package** : `io.github.quizup.notification`

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le **`quizup-bff`**.

## 3. Use cases (ports entrants — `domain/`)

- `NotificationAggregate` : `CreateNotificationCommand` (système, identifiant déterministe),
  `MarkNotificationReadCommand` (destinataire uniquement), `DeleteNotificationCommand`
  (destinataire uniquement — `NotificationDeletedEvent` puis `AggregateLifecycle.markDeleted()`).
- `NotificationPreferenceAggregate` : `UpdateNotificationPreferenceCommand`
  (`@CreationPolicy(CREATE_IF_MISSING)`).
- Commandes sans état : `MarkAllNotificationsReadCommand` (fan-out sur les ids non lus),
  `DeleteAllNotificationsCommand` (fan-out du hard delete sur tous les ids du joueur).
- Queries : `GetNotificationsQuery`, `GetNotificationQuery`, `CountUnreadNotificationsQuery`,
  `GetNotificationPreferencesQuery`, `GetUnreadNotificationIdsQuery`.

---

## 4. Ingestion (Kafka)

`NotificationIngestionHandler` (`@ProcessingGroup("notification-ingestion")`) consomme :

| Événement                       | Notification                                           |
|---------------------------------|--------------------------------------------------------|
| `UserFollowedEvent`             | `FOLLOW` pour le joueur suivi                          |
| `ChallengeCreatedEvent`         | `CHALLENGE_RECEIVED` pour l'invité (sourceId = challengeId) |
| `ChallengeAcceptedEvent`        | — (invitation expirée ; la salle émet ensuite `LOBBY_ACCEPTED`) |
| `ChallengeDeclinedEvent`        | `CHALLENGE_DECLINED` pour le lanceur                   |
| `LobbyCreatedEvent` (nominatif) | — (index de routage seul ; l'invitation vit dans le défi) |
| `LobbyJoinedEvent`              | `LOBBY_ACCEPTED` pour l'initiateur                     |
| `LobbyCompletedEvent`           | — mais rattache le `gameId` créé aux `LOBBY_ACCEPTED` (deep link arène) |
| `LobbyMissedEvent`              | `LOBBY_MISSED` pour le joueur qui a attendu (acteur = absent) |
| `LobbyDeclinedEvent`            | `LOBBY_DECLINED` pour l'initiateur                     |
| `LobbyCancelledEvent`           | — (invitation en attente expirée, aucune notification) |
| `LobbyExpiredEvent`             | — (invitation en attente expirée, aucune notification) |

> Les types `LOBBY_CANCELLED` et `LOBBY_EXPIRED` ne sont **plus produits** (« défi raté » sans
> valeur ajoutée) ; les lignes historiques restent affichables côté client.

- **Idempotence** : `notificationId = UUID.nameUUIDFromBytes(type + ":" + sourceId + ":" + userId)`
    + garde d'existence + contrainte unique `uq_notification_source` ; une relecture Kafka
      at-least-once ne duplique pas.
- **Routage** : `notification_routing_entry` (alimenté par `LobbyCreated/Joined`) résout les
  destinataires des événements terminaux qui ne les portent pas. L'index et la création vivent
  dans le **même processing group** (ordre par agrégat garanti).
- L'appariement public (`Matchmaking*`) n'est **pas** ingéré : l'écran de recherche bascule en
  direct vers l'arène (aucune notification d'appariement).
- **Suppression** : hard delete du read model (`notification_entry`) + `markDeleted()` de
  l'agrégat. La création est idempotente **même après suppression** : `createIfAllowed` vérifie le
  read model **et** l'event store (`EventStore.readEvents`) — une relecture Kafka at-least-once
  postérieure à une suppression est ignorée au lieu de recréer un agrégat existant (sinon
  `AggregateStreamCreationException` → poison pill du tracking processor). Un `catch` ciblé sert de
  filet en cas de course.
- **Invitations** : à la clôture d'un salon (rejoint, refusé, annulé, expiré, échoué, complété,
  purgé), l'invitation en attente est **expirée dans le read model** (`expiresAt` = instant de
  l'événement) : le client masque Accepter/Refuser et l'acceptation tardive (salon purgé → 404)
  est évitée. `LobbyFailedEvent` est consommé pour ce seul usage (pas de notification
  `LOBBY_FAILED`).
- **Deep link partie** : sur `LobbyCompletedEvent`, `attachGameId(sourceId, gameId)` met à jour les
  `LOBBY_ACCEPTED` du salon — l'inbox peut rejoindre l'arène même après la purge du salon (2 min).

---

## 5. Dépendances inter-services

Contrats consommés (pins release) : `quizup-social-domain`, `quizup-matchmaking-domain`.
Aucune query sortante : le service n'écrit que dans sa base et publie ses propres événements.

---

## 6. Contrats BFF

- `GET /api/notifications?unreadOnly=&page=&size=` → `PageResponse<NotificationView>`.
- `GET /api/notifications/unread-count` → `{ count }`.
- `POST /api/notifications/{id}/read` (propriétaire uniquement) ; `POST /api/notifications/read-all`.
- `DELETE /api/notifications/{id}` (propriétaire uniquement, `204`) → hard delete.
- `DELETE /api/notifications` (`204`) → vide l'inbox du joueur courant (hard delete, fan-out).
- `GET /api/notification-preferences` ; `PUT /api/notification-preferences/{category}`.
- WS `/topic/notifications/{userId}` (payload `NotificationView` dans un `EventEnvelopeResponse`,
  plus l'événement `NOTIFICATION_DELETED` à la suppression).

Read models : `notification_entry`, `notification_preference_entry`,
`notification_routing_entry` (migration `V1__create_notification_schema.sql`).
