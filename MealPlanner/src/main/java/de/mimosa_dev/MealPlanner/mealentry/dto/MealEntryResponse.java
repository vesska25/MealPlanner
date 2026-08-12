package de.mimosa_dev.MealPlanner.mealentry.dto;

import de.mimosa_dev.MealPlanner.mealentry.MealEntry;
import de.mimosa_dev.MealPlanner.mealentry.MealEntryType;

import java.math.BigDecimal;
import java.time.Instant;

public record MealEntryResponse(
        Long id, MealEntryType type, Long cookedDishId, BigDecimal portionsEaten,
        BigDecimal kcal, BigDecimal proteinGrams, BigDecimal fatGrams, BigDecimal carbsGrams, Instant occurredAt) {

    public static MealEntryResponse from(MealEntry entry) {
        return new MealEntryResponse(
                entry.getId(), entry.getType(), entry.getCookedDishId(), entry.getPortionsEaten(),
                entry.getKcal(), entry.getProteinGrams(), entry.getFatGrams(), entry.getCarbsGrams(), entry.getOccurredAt());
    }
}
