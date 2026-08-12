package de.mimosa_dev.MealPlanner.recipe;

import java.math.BigDecimal;

/**
 * FR-81 (notification type 1: "a new suggestion is ready"). Lives in {@code recipe}, not
 * {@code telegram}, so this package never has to know Telegram exists — the dependency stays
 * one-directional (telegram -> recipe), same as the existing cooking -> recipe dependency.
 */
public record SuggestionActivatedEvent(Long userId, Long recipeId, String recipeName, Integer basePortions, BigDecimal score) {
}
