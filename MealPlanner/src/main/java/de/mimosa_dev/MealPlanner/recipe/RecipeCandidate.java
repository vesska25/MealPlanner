package de.mimosa_dev.MealPlanner.recipe;

import java.util.List;
import java.util.Set;

/**
 * A model-generated recipe, in memory only, not yet persisted. The real {@code recipe} /
 * {@code recipe_ingredient} entities (PRD section 6) belong to the recipe-generation work in
 * step 6/7; this shape carries exactly what {@link RecipeValidator} needs to check (AI-08).
 *
 * @param requiredEquipment free-form equipment names, matched case-insensitively against
 *                           {@link UserConstraints#availableEquipment()} — the PRD does not
 *                           define a fixed equipment enum
 */
public record RecipeCandidate(
        String name,
        List<RecipeIngredient> ingredients,
        Set<String> requiredEquipment,
        int cookTimeMinutes) {

    /**
     * Reconstructs the validation-layer shape from an already-persisted {@link Recipe} (PRD
     * step 10's fallback, AI-20a/b — re-checking a past recipe against hard constraints and
     * FR-33 needs this same DTO). {@code recipe} must have its ingredients loaded (e.g. via
     * {@link RecipeRepository#findWithIngredientsById}), not the lazy default.
     */
    public static RecipeCandidate fromRecipe(Recipe recipe) {
        List<RecipeIngredient> ingredients = recipe.getIngredients().stream()
                .map(ingredient -> new RecipeIngredient(
                        ingredient.getProduct().getId(), ingredient.getQuantity(), ingredient.getUnit()))
                .toList();
        return new RecipeCandidate(recipe.getName(), ingredients, recipe.getRequiredEquipment(), recipe.getCookTimeMinutes());
    }
}
