package de.mimosa_dev.MealPlanner.profile.dto;

import de.mimosa_dev.MealPlanner.profile.GoalTargets;

import java.math.BigDecimal;

/** FR-70/71: the computed calorie/macro targets. Only ever returned when FR-74's toggle is on. */
public record GoalsResponse(
        BigDecimal dailyKcal, BigDecimal proteinGrams, BigDecimal fatGrams, BigDecimal carbsGrams,
        boolean flooredToMinimum) {

    public static GoalsResponse from(GoalTargets targets) {
        return new GoalsResponse(
                targets.dailyKcal(), targets.proteinGrams(), targets.fatGrams(), targets.carbsGrams(),
                targets.flooredToMinimum());
    }
}
