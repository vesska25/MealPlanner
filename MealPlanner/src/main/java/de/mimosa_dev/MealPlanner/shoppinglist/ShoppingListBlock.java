package de.mimosa_dev.MealPlanner.shoppinglist;

/** FR-42's two blocks. */
public enum ShoppingListBlock {
    /** Missing quantity of a non-staple ingredient the active suggestion needs. */
    DEFINITELY_NEED,
    /** A staple ingredient the active suggestion needs — FR-29 excludes staples from pantry
     * accounting entirely, so the system genuinely doesn't know if the user has it. */
    CHECK_MAYBE_OUT
}
