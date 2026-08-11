package de.mimosa_dev.MealPlanner.account.dto;

import de.mimosa_dev.MealPlanner.common.Unit;

import java.math.BigDecimal;

public record RecipeIngredientExport(String productName, BigDecimal quantity, Unit unit) {
}
