package de.mimosa_dev.MealPlanner.cooking.dto;

import de.mimosa_dev.MealPlanner.cooking.CookedDish;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CookedDishResponse(
        Long id, Long recipeId, String recipeName, String category,
        BigDecimal totalPortions, BigDecimal portionsRemaining,
        BigDecimal kcalPerPortion, BigDecimal proteinPerPortion, BigDecimal fatPerPortion, BigDecimal carbsPerPortion,
        LocalDate cookedAt, LocalDate expiresAt, String status) {

    public static CookedDishResponse from(CookedDish dish) {
        return new CookedDishResponse(
                dish.getId(), dish.getRecipe().getId(), dish.getRecipe().getName(), dish.getCategory().name(),
                dish.getTotalPortions(), dish.getPortionsRemaining(),
                dish.getKcalPerPortion(), dish.getProteinPerPortion(), dish.getFatPerPortion(), dish.getCarbsPerPortion(),
                dish.getCookedAt(), dish.getExpiresAt(), dish.getStatus().name());
    }
}
