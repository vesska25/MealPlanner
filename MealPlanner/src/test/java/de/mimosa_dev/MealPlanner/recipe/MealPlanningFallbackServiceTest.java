package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.cooking.CookedDish;
import de.mimosa_dev.MealPlanner.cooking.CookedDishRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import({MealPlanningFallbackService.class, RecipeValidator.class, RecipeCandidateScorer.class, PantryService.class})
class MealPlanningFallbackServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MealPlanningFallbackService fallbackService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private CookedDishRepository cookedDishRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void picksTheFeasibleRecipeOverAnInfeasibleOne() {
        Product milk = seeded("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("1000"), Unit.GRAM, LocalDate.now().plusDays(30));
        saveRecipeWithIngredient("Feasible soup", milk, "200");

        Product chicken = seeded("chicken breast"); // no pantry stock given at all
        saveRecipeWithIngredient("Infeasible roast", chicken, "500");
        entityManager.flush();
        entityManager.clear();

        var pick = fallbackService.selectFallback(USER_ID);

        assertThat(pick).isPresent();
        assertThat(pick.get().recipe().getName()).isEqualTo("Feasible soup");
    }

    @Test
    void excludesARecipeCookedWithinTheLastFourteenDays() {
        Product milk = seeded("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("1000"), Unit.GRAM, LocalDate.now().plusDays(30));
        var recipe = saveRecipeWithIngredient("Recently made stew", milk, "200");

        LocalDate cookedAt = LocalDate.now().minusDays(2);
        cookedDishRepository.save(new CookedDish(
                USER_ID, recipe, DishCategory.UNKNOWN, new BigDecimal("2"),
                null, null, null, null, cookedAt, cookedAt.plusDays(2), "key-fallback-recency"));
        entityManager.flush();
        entityManager.clear();

        assertThat(fallbackService.selectFallback(USER_ID)).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheUserHasNoRecipesAtAll() {
        assertThat(fallbackService.selectFallback(USER_ID)).isEmpty();
    }

    private Product seeded(String canonicalName) {
        return productRepository.findByCanonicalNameIgnoreCase(canonicalName).orElseThrow();
    }

    private Recipe saveRecipeWithIngredient(String name, Product product, String quantity) {
        Recipe recipe = new Recipe(USER_ID, name, 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(product, new BigDecimal(quantity), Unit.GRAM));
        return recipeRepository.save(recipe);
    }
}
