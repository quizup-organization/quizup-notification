package io.github.quizup.notification.domain.exception;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;

import java.util.Map;

/** Classe de base des exceptions métier du domaine notification. */
public abstract class NotificationProblem extends BaseProblem {

    protected NotificationProblem(String type,
                                  ProblemCategory category,
                                  String title,
                                  String detail,
                                  Map<String, Object> context) {
        super(type, category, title, detail, context);
    }
}
