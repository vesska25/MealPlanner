package de.mimosa_dev.MealPlanner.mealentry.dto;

import de.mimosa_dev.MealPlanner.mealentry.MealEntryType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateMealEntryRequest(
        @NotNull MealEntryType type, BigDecimal kcal, BigDecimal proteinGrams, BigDecimal fatGrams, BigDecimal carbsGrams) {
}
