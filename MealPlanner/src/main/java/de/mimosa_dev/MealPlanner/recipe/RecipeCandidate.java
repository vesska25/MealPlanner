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
}
