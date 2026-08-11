package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.product.ProductCategory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Determines a cooked dish's category from its recipe's ingredients by a fixed, deterministic
 * rule (FR-52e) — never chosen by the model. Priority: fish beats meat beats vegetable beats
 * grain/pasta; anything that doesn't match falls to {@link DishCategory#UNKNOWN}, which FR-52e
 * explicitly allows as a conservative default for later reference-dictionary refinement.
 *
 * <p>{@link DishCategory#SOUP} is deliberately unreachable here: composition alone (which
 * products a recipe contains) doesn't reliably distinguish "soup" from any other dish using the
 * same ingredients, and the PRD doesn't ask for a guess — {@code UNKNOWN}'s conservative
 * 2-day default is the correct fallback until a better signal (e.g. an explicit recipe field)
 * exists.
 */
@Component
public class DishCategoryResolver {

    private static final Set<ProductCategory> GRAIN_PASTA_SOURCES =
            EnumSet.of(ProductCategory.GRAIN, ProductCategory.LEGUME, ProductCategory.BAKERY);

    public DishCategory resolve(Recipe recipe) {
        Set<ProductCategory> ingredientCategories = recipe.getIngredients().stream()
                .map(ingredient -> ingredient.getProduct().getCategory())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ProductCategory.class)));

        if (ingredientCategories.contains(ProductCategory.FISH)) {
            return DishCategory.FISH;
        }
        if (ingredientCategories.contains(ProductCategory.MEAT)) {
            return DishCategory.MEAT;
        }
        if (ingredientCategories.contains(ProductCategory.PRODUCE)) {
            return DishCategory.VEGETABLE;
        }
        if (!Collections.disjoint(ingredientCategories, GRAIN_PASTA_SOURCES)) {
            return DishCategory.GRAIN_PASTA;
        }
        return DishCategory.UNKNOWN;
    }
}
