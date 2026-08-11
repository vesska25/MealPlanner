package de.mimosa_dev.MealPlanner.recipe;

/**
 * FR-60's five rejection reasons. {@code NOT_TODAY} is recorded on the {@link RecipeSuggestion}
 * itself but never turns into a {@link PreferenceSignal} (FR-62: it doesn't affect the
 * long-term preference model).
 */
public enum RejectionReason {
    DISLIKE_DISH,
    NOT_TODAY,
    TAKES_TOO_LONG,
    DONT_WANT_CATEGORY,
    TIRED_OF_INGREDIENT
}
