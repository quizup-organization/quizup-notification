package io.github.quizup.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Service headless de notifications : inbox par joueur (lu/non-lu), préférences et ingestion
 * des événements de domaine (follows, salons, appariement). La surface REST/WS est portée par
 * le BFF.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
