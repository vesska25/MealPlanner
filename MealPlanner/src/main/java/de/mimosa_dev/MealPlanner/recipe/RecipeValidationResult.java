package de.mimosa_dev.MealPlanner.recipe;

import java.util.List;

/** Outcome of {@link RecipeValidator#validate}. All violations are collected, not just the first. */
public record RecipeValidationResult(boolean valid, List<RecipeViolation> violations) {

    public static RecipeValidationResult success() {
        return new RecipeValidationResult(true, List.of());
    }

    public static RecipeValidationResult invalid(List<RecipeViolation> violations) {
        return new RecipeValidationResult(false, violations);
    }
}
