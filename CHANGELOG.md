## [3.5.1](https://github.com/quizup-organization/quizup-notification/compare/v3.5.0...v3.5.1) (2026-10-12)

### Bug Fixes

* **notification:** contrats matchmaking 10.2.0 / social 5.2.0 (élagués) ([730a2e2](https://github.com/quizup-organization/quizup-notification/commit/730a2e2e32cb19b7472ba5e0bbc9c81f9fa93267))

## [3.5.0](https://github.com/quizup-organization/quizup-notification/compare/v3.4.0...v3.5.0) (2026-10-12)

### Features

* **console:** consume quizup-sdk 4.7.0 (lag, commandes en vol, snapshots, scheduled) ([84cdee7](https://github.com/quizup-organization/quizup-notification/commit/84cdee7abfc0baadc7003ba047d2b5a0839d9a49))

## [3.4.0](https://github.com/quizup-organization/quizup-notification/compare/v3.3.0...v3.4.0) (2026-10-12)

### Features

* **console:** consume quizup-sdk 4.6.0 (build-info + API interne event store/scheduled) ([02db4aa](https://github.com/quizup-organization/quizup-notification/commit/02db4aa68d2f57306fe25ad4e049c6516a9c6109))

## [3.3.0](https://github.com/quizup-organization/quizup-notification/compare/v3.2.0...v3.3.0) (2026-10-12)

### Features

* **console:** consume quizup-sdk 4.5.2 (DLQ Axon par défaut + agent console) ([9e52082](https://github.com/quizup-organization/quizup-notification/commit/9e52082414160a0a161fb41c9b9e8c6dc73221bf))

## [3.2.0](https://github.com/quizup-organization/quizup-notification/compare/v3.1.0...v3.2.0) (2026-10-11)

### Features

* **notification:** l'annulation d'un défi supprime l'invitation (push temps réel) ([0d25648](https://github.com/quizup-organization/quizup-notification/commit/0d256480846322179cf2bbb850209bc0c43a33e9))

## [3.1.0](https://github.com/quizup-organization/quizup-notification/compare/v3.0.1...v3.1.0) (2026-10-11)

### Features

* **notification:** l'invitation de revanche porte le gameId d'origine ([68130c6](https://github.com/quizup-organization/quizup-notification/commit/68130c663590e603313ac1cc849ffd1da16ecfdd))

## [3.0.1](https://github.com/quizup-organization/quizup-notification/compare/v3.0.0...v3.0.1) (2026-10-11)

### Bug Fixes

* **notification:** aligne l'ingestion sur les contrats matchmaking 7.0.0 ([8d196fe](https://github.com/quizup-organization/quizup-notification/commit/8d196fe5318e0e2c7a2c8f4e782aa463346ebdaf))

## [3.0.0](https://github.com/quizup-organization/quizup-notification/compare/v2.7.0...v3.0.0) (2026-10-10)

### ⚠ BREAKING CHANGES

* **notification:** types/catégories de notification et schéma V1 réécrits.

### Features

* **notification:** types ROOM_*, routage supprimé, ingestion allégée ([787605d](https://github.com/quizup-organization/quizup-notification/commit/787605df1451d2e61f6feddec0f5a77147f7a852))

## [2.7.0](https://github.com/quizup-organization/quizup-notification/compare/v2.6.0...v2.7.0) (2026-10-10)

### Features

* **notification:** marquer une notification en non lue ([15e147c](https://github.com/quizup-organization/quizup-notification/commit/15e147ca810be8fe85a47d9db699e0d680a15fbd))

## [2.6.0](https://github.com/quizup-organization/quizup-notification/compare/v2.5.0...v2.6.0) (2026-10-08)

### Features

* **notification:** expire les invitations a la purge d'un defi ([bc5ee60](https://github.com/quizup-organization/quizup-notification/commit/bc5ee60c4398f48d3daf8ddbda85c7ea01ebc532))

## [2.5.0](https://github.com/quizup-organization/quizup-notification/compare/v2.4.0...v2.5.0) (2026-10-05)

### Features

* **notification:** suppression en masse de l'inbox (DeleteAllNotificationsCommand) ([a126098](https://github.com/quizup-organization/quizup-notification/commit/a1260989e5845e9e0671d0f50016d6389ce2a142))

## [2.4.0](https://github.com/quizup-organization/quizup-notification/compare/v2.3.0...v2.4.0) (2026-10-05)

### Features

* **notification:** ingestion du defi nominatif (CHALLENGE_RECEIVED/DECLINED) ([a4d40d8](https://github.com/quizup-organization/quizup-notification/commit/a4d40d81c7c4c73a3b8533b69fb95056304f1b14))

## [2.3.0](https://github.com/quizup-organization/quizup-notification/compare/v2.2.2...v2.3.0) (2026-10-05)

### Features

* **notification:** notification durable LOBBY_MISSED ([8d0ae83](https://github.com/quizup-organization/quizup-notification/commit/8d0ae8364db4a42988d5edd617b82669167e0247))

## [2.2.2](https://github.com/quizup-organization/quizup-notification/compare/v2.2.1...v2.2.2) (2026-10-05)

### Bug Fixes

* **notification:** rattache le gameId aux notifications d'acceptation ([fbef047](https://github.com/quizup-organization/quizup-notification/commit/fbef04741f779ff343a0cdb911168e27c7c064ee))

## [2.2.1](https://github.com/quizup-organization/quizup-notification/compare/v2.2.0...v2.2.1) (2026-10-05)

### Bug Fixes

* **notification:** ingestion idempotente face au replay apres suppression ([cef4a96](https://github.com/quizup-organization/quizup-notification/commit/cef4a960ed0742c5d15e8d366e28552f6170711d))

## [2.2.0](https://github.com/quizup-organization/quizup-notification/compare/v2.1.0...v2.2.0) (2026-10-04)

### Features

* **notification:** expiration des invitations closes et suppression des notifications d'annulation ([95846a7](https://github.com/quizup-organization/quizup-notification/commit/95846a73c4e0f45719790ee12805d1598427a11f))

## [2.1.0](https://github.com/quizup-organization/quizup-notification/compare/v2.0.0...v2.1.0) (2026-10-04)

### Features

* **notification:** suppression des notifications par le destinataire (hard delete) ([ad3828e](https://github.com/quizup-organization/quizup-notification/commit/ad3828edcb23d3cee4a6397d4a9304293803ff85))

## [2.0.0](https://github.com/quizup-organization/quizup-notification/compare/v1.0.2...v2.0.0) (2026-10-03)

### ⚠ BREAKING CHANGES

* **notification:** drop matchmaking notifications

### Features

* **notification:** drop matchmaking notifications ([435e7a1](https://github.com/quizup-organization/quizup-notification/commit/435e7a1917fb52bab5f9cdb52868446ca4e95591))

## [1.0.2](https://github.com/quizup-organization/quizup-notification/compare/v1.0.1...v1.0.2) (2026-10-03)

### Bug Fixes

* **notification:** notify invitee when a nominative lobby expires ([6581d6e](https://github.com/quizup-organization/quizup-notification/commit/6581d6e8c9a64db92bc64ce74bb8a18045262a8d))

## [1.0.1](https://github.com/quizup-organization/quizup-notification/compare/v1.0.0...v1.0.1) (2026-10-03)

### Bug Fixes

* **notification:** pin matchmaking-domain 4.1.0 ([e9c07f7](https://github.com/quizup-organization/quizup-notification/commit/e9c07f7a138a2b22a89c12e872fca9da5661170d))

## 1.0.0 (2026-10-03)

### Features

* **notification:** add player notification service ([4677230](https://github.com/quizup-organization/quizup-notification/commit/467723076c5002a9905618fb8a859d38c3a923a1))
