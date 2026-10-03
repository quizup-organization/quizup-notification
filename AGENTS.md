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
  expiration, lu/non-lu).
- **Préférences** : une catégorie (`FOLLOW`, `LOBBY`, `MATCHMAKING`) peut être coupée ; défaut
  activé. Une notification dont la catégorie est coupée n'est pas créée.
- **Livraison** : le service publie `NotificationCreatedEvent` sur le bus ; le **BFF** le pousse
  sur `/topic/notifications/{userId}` et l'expose en REST (`/api/notifications`,
  `/api/notification-preferences`).

**Package** : `io.github.quizup.notification`

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`**.

## 3. Use cases (ports entrants — `domain/`)

- `NotificationAggregate` : `CreateNotificationCommand` (système, identifiant déterministe),
  `MarkNotificationReadCommand` (destinataire uniquement).
- `NotificationPreferenceAggregate` : `UpdateNotificationPreferenceCommand`
  (`@CreationPolicy(CREATE_IF_MISSING)`).
- Commandes sans état : `MarkAllNotificationsReadCommand` (fan-out sur les ids non lus).
- Queries : `GetNotificationsQuery`, `GetNotificationQuery`, `CountUnreadNotificationsQuery`,
  `GetNotificationPreferencesQuery`, `GetUnreadNotificationIdsQuery`.

---

## 4. Ingestion (Kafka)

`NotificationIngestionHandler` (`@ProcessingGroup("notification-ingestion")`) consomme :

| Événement | Notification |
|---|---|
| `UserFollowedEvent` | `FOLLOW` pour le joueur suivi |
| `LobbyCreatedEvent` (nominatif) | `LOBBY_INVITATION` pour l'invité (+ index de routage) |
| `LobbyJoinedEvent` | `LOBBY_ACCEPTED` pour l'initiateur |
| `LobbyDeclinedEvent` | `LOBBY_DECLINED` pour l'initiateur |
| `LobbyCancelledEvent` | `LOBBY_CANCELLED` pour l'autre participant (selon `reason`) |
| `LobbyExpiredEvent` | `LOBBY_EXPIRED` pour l'initiateur |
| `MatchmakingMatchedEvent` | `MATCHMAKING_READY` pour le joueur du ticket |

- **Idempotence** : `notificationId = UUID.nameUUIDFromBytes(type + ":" + sourceId + ":" + userId)`
  + garde d'existence + contrainte unique `uq_notification_source` ; une relecture Kafka
  at-least-once ne duplique pas.
- **Routage** : `notification_routing_entry` (alimenté par `LobbyCreated/Joined`,
  `MatchmakingStarted`) résout les destinataires des événements terminaux qui ne les portent pas.
  L'index et la création vivent dans le **même processing group** (ordre par agrégat garanti).
- Les notifications **ne sont pas supprimées** en v1 (pas de TTL) ; seule la date de lecture
  évolue.

---

## 5. Dépendances inter-services

Contrats consommés (pins release) : `quizup-social-domain`, `quizup-matchmaking-domain`.
Aucune query sortante : le service n'écrit que dans sa base et publie ses propres événements.

---

## 6. Contrats BFF

- `GET /api/notifications?unreadOnly=&page=&size=` → `PageResponse<NotificationView>`.
- `GET /api/notifications/unread-count` → `{ count }`.
- `POST /api/notifications/{id}/read` (propriétaire uniquement) ; `POST /api/notifications/read-all`.
- `GET /api/notification-preferences` ; `PUT /api/notification-preferences/{category}`.
- WS `/topic/notifications/{userId}` (payload `NotificationView` dans un `EventEnvelopeResponse`).

Read models : `notification_entry`, `notification_preference_entry`,
`notification_routing_entry` (migration `V1__create_notification_schema.sql`).
