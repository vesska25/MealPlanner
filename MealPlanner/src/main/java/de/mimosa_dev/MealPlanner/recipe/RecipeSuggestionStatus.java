package de.mimosa_dev.MealPlanner.recipe;

/** FR-24's suggestion lifecycle. Only one row is ever {@code ACTIVE} per user at a time. */
public enum RecipeSuggestionStatus {
    ACTIVE,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
