# quizup-notification

Service headless de notifications personnelles : inbox lu/non-lu, préférences par catégorie et
ingestion des événements de domaine (follows, salons privés, appariement). La surface REST + WS
est portée par `quizup-bff`.

## Build

```bash
mvn test          # tests unitaires (agrégats) + compilation
mvn install       # installe domain + infrastructure dans le .m2 local
```

## Documentation

Voir [`AGENTS.md`](AGENTS.md) pour le rôle, les use cases, l'ingestion Kafka et les contrats BFF.
