package edu.nie.campusfix.service;

import edu.nie.campusfix.enums.Category;
import edu.nie.campusfix.enums.Priority;
import edu.nie.campusfix.enums.Severity;
import java.util.Objects;

/**
 * Deterministic, rule-based complaint prioritization engine.
 * Calculates priority using:
 * baseScore = severity.getBaseScore() (LOW=1, MEDIUM=2, HIGH=3)
 * bonus = +1 if category is high risk (ELECTRICAL or SAFETY)
 * score = clamp(baseScore + bonus, 1, 4)
 * priority = 1: LOW, 2: MEDIUM, 3: HIGH, 4: CRITICAL
 */
public class PriorityService {

    /**
     * Calculates the deterministic Priority for a given severity and category combination.
     */
    public Priority calculatePriority(Severity severity, Category category) {
        Objects.requireNonNull(severity, "Severity cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");

        int score = severity.getBaseScore();
        if (category.isHighRisk()) {
            score += 1;
        }
        return Priority.fromScore(score);
    }

    /**
     * Generates a transparent, user-readable explanation of how the priority was derived.
     */
    public String explainCalculation(Severity severity, Category category) {
        Objects.requireNonNull(severity, "Severity cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");

        Priority result = calculatePriority(severity, category);
        int base = severity.getBaseScore();
        int bonus = category.isHighRisk() ? 1 : 0;
        int finalScore = Math.min(4, Math.max(1, base + bonus));

        StringBuilder explanation = new StringBuilder();
        explanation.append("Base score: ").append(base).append(" (").append(severity.getDisplayName()).append(" severity)");
        if (bonus > 0) {
            explanation.append(" + 1 high-risk bonus (").append(category.getDisplayName()).append(")");
        }
        explanation.append(" = ").append(finalScore).append(" -> Priority: ").append(result.getDisplayName());
        explanation.append(" (Response target: ").append(result.getResponseTargetHours()).append("h)");
        return explanation.toString();
    }
}
