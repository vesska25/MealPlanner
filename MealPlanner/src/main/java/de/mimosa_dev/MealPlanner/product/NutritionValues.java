package de.mimosa_dev.MealPlanner.product;

import java.math.BigDecimal;

/** Result of {@link NutritionCalculationService#calculate}, for a specific quantity of a product. */
public record NutritionValues(BigDecimal kcal, BigDecimal proteinGrams, BigDecimal fatGrams, BigDecimal carbsGrams) {
}
