package de.mimosa_dev.MealPlanner.recipe;

import java.util.Set;

/**
 * The hard constraints a recipe must satisfy for one user (PRD 3.2, AI-08). Stand-in for the
 * real {@code user_profile} entity, which doesn't exist yet (onboarding/auth is PRD step 9) —
 * these are exactly the fields step 5's validator needs, nothing more.
 *
 * @param excludedProductIds allergies (FR-14) and explicit "never" exclusions (FR-63) combined
 *                            — both are absolute vetoes at this layer; the distinction between
 *                            them only matters for the soft-preference system, not here
 * @param availableEquipment free-form equipment names, matched case-insensitively
 */
public record UserConstraints(
        Set<Long> excludedProductIds,
        Set<String> availableEquipment,
        int maxCookTimeMinutes) {
}
