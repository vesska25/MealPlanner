package de.mimosa_dev.MealPlanner.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CookedDishExport(
        Long id, Long recipeId, String category, BigDecimal totalPortions, BigDecimal portionsRemaining,
        BigDecimal kcalPerPortion, BigDecimal proteinPerPortion, BigDecimal fatPerPortion, BigDecimal carbsPerPortion,
        LocalDate cookedAt, LocalDate expiresAt, String status) {
}
