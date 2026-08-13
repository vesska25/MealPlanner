package de.mimosa_dev.MealPlanner.profile;

import java.math.BigDecimal;

/**
 * Result of {@link GoalCalculationService#calculate} — structured numbers only, no user-facing
 * wording. Explaining a floored target (FR-71) or an extreme-request disclaimer (FR-71a) is the
 * onboarding LLM's job (AI-01); it reads {@code flooredToMinimum} as ground truth it cannot
 * alter (AI-07).
 */
public record GoalTargets(
        BigDecimal dailyKcal, BigDecimal proteinGrams, BigDecimal fatGrams, BigDecimal carbsGrams,
        boolean flooredToMinimum) {
}
