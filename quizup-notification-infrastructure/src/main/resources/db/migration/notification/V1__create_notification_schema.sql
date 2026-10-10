-- V1: Schéma notification — inbox par joueur et préférences.
-- Les notifications sont dérivées des événements de domaine (id déterministe) : une relecture
-- Kafka (at-least-once) est idempotente grâce à la PK et à la contrainte unique.

CREATE TABLE notification_entry (
    notification_id     VARCHAR(255) NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    type                VARCHAR(40)  NOT NULL,
    actor_id            VARCHAR(255),
    source_id           VARCHAR(255),
    topic_id            VARCHAR(255),
    game_id             VARCHAR(255),
    expires_at          TIMESTAMP,
    read_at             TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL,
    PRIMARY KEY (notification_id)
);

CREATE INDEX idx_notification_user ON notification_entry (user_id, created_at);
CREATE INDEX idx_notification_unread ON notification_entry (user_id, read_at);
CREATE UNIQUE INDEX uq_notification_source ON notification_entry (type, source_id, user_id);

CREATE TABLE notification_preference_entry (
    user_id    VARCHAR(255) NOT NULL,
    category   VARCHAR(40)  NOT NULL,   -- FOLLOW, ROOM
    enabled    BOOLEAN      NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (user_id, category)
);
