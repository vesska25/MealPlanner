package de.mimosa_dev.MealPlanner.recipe;

/**
 * A cooked dish's category (FR-52a/FR-52e), used only to look up its shelf life in
 * {@code dish_category_shelf_life}. {@code SOUP} exists for that table's completeness but is
 * not reachable by {@link DishCategoryResolver}'s v1 rule — see its Javadoc.
 */
public enum DishCategory {
    FISH,
    MEAT,
    VEGETABLE,
    GRAIN_PASTA,
    SOUP,
    UNKNOWN
}
