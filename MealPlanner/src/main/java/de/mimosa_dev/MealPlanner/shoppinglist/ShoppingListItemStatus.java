package de.mimosa_dev.MealPlanner.shoppinglist;

/**
 * FR-44/FR-45/FR-45a: {@code PENDING} is the default, unresolved state — left there, an item
 * implies nothing about pantry contents (FR-45a's "unmarked unchecked box" in practice).
 * {@code PURCHASED} and {@code ALREADY_HAVE} both add the item to pantry as an estimate
 * (FR-45b, and acceptance criterion 2 for the checkbox case); {@code NOT_BUYING} and
 * {@code NOT_NEEDED} never touch pantry.
 */
public enum ShoppingListItemStatus {
    PENDING,
    PURCHASED,
    ALREADY_HAVE,
    NOT_BUYING,
    NOT_NEEDED
}
