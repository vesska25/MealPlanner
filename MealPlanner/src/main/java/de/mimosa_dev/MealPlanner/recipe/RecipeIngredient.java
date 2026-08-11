package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.common.Unit;

import java.math.BigDecimal;

/**
 * One ingredient line of a model-generated recipe candidate, referencing an existing
 * catalogue {@code Product} by id. Quantity/unit are already canonical here (AI-04) — the
 * raw-JSON tool-call parsing boundary that enforces that on the model's actual output belongs
 * to the agent layer (PRD step 6), not to this validator.
 */
public record RecipeIngredient(Long productId, BigDecimal quantity, Unit unit) {
}
