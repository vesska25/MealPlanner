package de.mimosa_dev.MealPlanner.common;

/**
 * Canonical quantity units (AI-04). Free-form quantity phrasing is never accepted;
 * every quantity crossing a tool-call boundary is normalized to one of these.
 * Used across pantry, shopping list, recipe, and nutrition calculations (AI-04a).
 */
public enum Unit {
    GRAM,
    MILLILITER,
    PIECE
}
