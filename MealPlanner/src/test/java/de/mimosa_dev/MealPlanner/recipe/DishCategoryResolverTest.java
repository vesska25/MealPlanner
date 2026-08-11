package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(DishCategoryResolver.class)
class DishCategoryResolverTest extends AbstractIntegrationTest {

    @Autowired
    private DishCategoryResolver resolver;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void aFishIngredientTakesPriorityOverEverythingElse() {
        Recipe recipe = recipeOf(seeded("salmon"), seeded("chicken breast"), seeded("rice"));

        assertThat(resolver.resolve(recipe)).isEqualTo(DishCategory.FISH);
    }

    @Test
    void aMeatIngredientTakesPriorityOverVegetableAndGrain() {
        Recipe recipe = recipeOf(seeded("chicken breast"), seeded("onion"), seeded("rice"));

        assertThat(resolver.resolve(recipe)).isEqualTo(DishCategory.MEAT);
    }

    @Test
    void aVegetableIngredientTakesPriorityOverGrain() {
        Recipe recipe = recipeOf(seeded("onion"), seeded("rice"));

        assertThat(resolver.resolve(recipe)).isEqualTo(DishCategory.VEGETABLE);
    }

    @Test
    void grainLegumeOrBakeryIngredientsResolveToGrainPasta() {
        assertThat(resolver.resolve(recipeOf(seeded("rice")))).isEqualTo(DishCategory.GRAIN_PASTA);
        assertThat(resolver.resolve(recipeOf(seeded("lentils")))).isEqualTo(DishCategory.GRAIN_PASTA);
        assertThat(resolver.resolve(recipeOf(seeded("bread")))).isEqualTo(DishCategory.GRAIN_PASTA);
    }

    @Test
    void ingredientsOutsideTheKnownPriorityListFallBackToUnknown() {
        Recipe recipe = recipeOf(seeded("olive oil"), seeded("soy sauce"));

        assertThat(resolver.resolve(recipe)).isEqualTo(DishCategory.UNKNOWN);
    }

    @Test
    void aRecipeWithNoIngredientsFallsBackToUnknown() {
        Recipe recipe = new Recipe(1L, "Empty", 5, 1, Set.of());

        assertThat(resolver.resolve(recipe)).isEqualTo(DishCategory.UNKNOWN);
    }

    private Product seeded(String canonicalName) {
        return productRepository.findByCanonicalNameIgnoreCase(canonicalName).orElseThrow();
    }

    private static Recipe recipeOf(Product... products) {
        Recipe recipe = new Recipe(1L, "Test recipe", 20, 4, Set.of());
        for (Product product : products) {
            recipe.addIngredient(new RecipeIngredientEntity(product, new BigDecimal("100"), Unit.GRAM));
        }
        return recipe;
    }
}
