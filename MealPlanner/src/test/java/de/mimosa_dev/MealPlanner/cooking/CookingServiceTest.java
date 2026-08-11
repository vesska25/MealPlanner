package de.mimosa_dev.MealPlanner.cooking;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.NutritionCalculationService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.recipe.DishCategoryResolver;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        CookingService.class, DishCategoryResolver.class, NutritionCalculationService.class,
        PantryService.class
})
class CookingServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private CookingService cookingService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void happyPathDeductsScaledIngredientsAndCreatesTheCookedDish() {
        Product milk = seeded("milk");
        Product rice = seeded("rice");
        givePantryStock(milk, "1000");
        givePantryStock(rice, "500");
        Recipe recipe = saveRecipe(2, ingredient(milk, "200"), ingredient(rice, "100"));
        entityManager.flush();
        entityManager.clear();

        CookedDish dish = cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("2"), "key-happy-path");

        assertThat(dish.getTotalPortions()).isEqualByComparingTo("2");
        assertThat(dish.getPortionsRemaining()).isEqualByComparingTo("2");
        assertThat(dish.getStatus()).isEqualTo(CookedDishStatus.ACTIVE);
        assertThat(dish.getExpiresAt()).isEqualTo(dish.getCookedAt().plusDays(3)); // GRAIN_PASTA shelf life
        assertThat(dish.getKcalPerPortion()).isNotNull().isPositive();
        assertThat(stockOf(milk)).isEqualByComparingTo("800");
        assertThat(stockOf(rice)).isEqualByComparingTo("400");
    }

    @Test
    void stapleIngredientsAreNeverCheckedOrDeductedFromPantry() {
        Product milk = seeded("milk");
        Product salt = seeded("salt"); // is_staple = true, deliberately given no pantry stock
        givePantryStock(milk, "1000");
        Recipe recipe = saveRecipe(1, ingredient(milk, "200"), ingredient(salt, "5"));
        entityManager.flush();
        entityManager.clear();

        CookedDish dish = cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("1"), "key-staple");

        assertThat(dish.getStatus()).isEqualTo(CookedDishStatus.ACTIVE);
        assertThat(stockOf(milk)).isEqualByComparingTo("800");
    }

    @Test
    void insufficientStockAfterScalingRejectsEverythingAndDeductsNothing() {
        Product milk = seeded("milk");
        Product rice = seeded("rice");
        givePantryStock(milk, "1000");
        givePantryStock(rice, "50"); // not enough once scaled below
        Recipe recipe = saveRecipe(1, ingredient(milk, "200"), ingredient(rice, "100"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("1"), "key-insufficient"))
                .isInstanceOf(CookingInfeasibleException.class)
                .satisfies(ex -> assertThat(((CookingInfeasibleException) ex).missingIngredients())
                        .extracting(CookingInfeasibleException.MissingIngredient::productId)
                        .containsExactly(rice.getId()));

        // FR-57: all-or-nothing — milk (which was sufficient) must be untouched too.
        assertThat(stockOf(milk)).isEqualByComparingTo("1000");
        assertThat(stockOf(rice)).isEqualByComparingTo("50");
        assertThat(cookedDishCount()).isZero();
    }

    @Test
    void doublingActualPortionsDeductsDoubleIngredientsWithUnchangedPerPortionNutrition() {
        Product milk = seeded("milk");
        givePantryStock(milk, "10000");
        Recipe recipe = saveRecipe(2, ingredient(milk, "200"));
        entityManager.flush();
        entityManager.clear();

        CookedDish singleBatch = cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("2"), "key-single");
        entityManager.flush();
        entityManager.clear();
        BigDecimal stockAfterFirst = stockOf(milk);

        CookedDish doubledBatch = cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("4"), "key-doubled");

        // recipe at basePortions=2 needs 200g milk; doubling to 4 actual portions needs 400g —
        // exactly double the deduction of the single-batch run above.
        BigDecimal deductedBySingleBatch = new BigDecimal("10000").subtract(stockAfterFirst);
        BigDecimal deductedByDoubledBatch = stockAfterFirst.subtract(stockOf(milk));
        assertThat(deductedByDoubledBatch).isEqualByComparingTo(deductedBySingleBatch.multiply(BigDecimal.valueOf(2)));

        assertThat(doubledBatch.getKcalPerPortion()).isEqualByComparingTo(singleBatch.getKcalPerPortion());
        assertThat(doubledBatch.getTotalPortions()).isEqualByComparingTo("4");
    }

    @Test
    void resubmittingTheSameIdempotencyKeyReturnsTheOriginalWithoutDeductingAgain() {
        Product milk = seeded("milk");
        givePantryStock(milk, "1000");
        Recipe recipe = saveRecipe(1, ingredient(milk, "200"));
        entityManager.flush();
        entityManager.clear();

        CookedDish first = cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("1"), "key-idempotent");
        entityManager.flush();
        entityManager.clear();
        CookedDish second = cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("1"), "key-idempotent");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(stockOf(milk)).isEqualByComparingTo("800"); // deducted once, not twice
        assertThat(cookedDishCount()).isEqualTo(1);
    }

    @Test
    void aRecipeWithOnlyUnrecognizedCategoryIngredientsFallsBackToTheDefaultShelfLife() {
        Product soySauce = seeded("soy sauce"); // CONDIMENT — not in the resolver's priority list
        givePantryStock(soySauce, "500");
        Recipe recipe = saveRecipe(1, ingredient(soySauce, "50"));
        entityManager.flush();
        entityManager.clear();

        CookedDish dish = cookingService.confirmCooking(USER_ID, recipe.getId(), new BigDecimal("1"), "key-unknown-category");

        assertThat(dish.getExpiresAt()).isEqualTo(dish.getCookedAt().plusDays(2)); // UNKNOWN default
    }

    private Product seeded(String canonicalName) {
        return productRepository.findByCanonicalNameIgnoreCase(canonicalName).orElseThrow();
    }

    private void givePantryStock(Product product, String quantity) {
        pantryService.addStock(USER_ID, product, new BigDecimal(quantity), Unit.GRAM, LocalDate.now());
    }

    private static RecipeIngredientEntity ingredient(Product product, String quantity) {
        return new RecipeIngredientEntity(product, new BigDecimal(quantity), Unit.GRAM);
    }

    private Recipe saveRecipe(int basePortions, RecipeIngredientEntity... ingredients) {
        Recipe recipe = new Recipe(USER_ID, "Test recipe", 20, basePortions, Set.of());
        for (RecipeIngredientEntity ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        return recipeRepository.save(recipe);
    }

    private BigDecimal stockOf(Product product) {
        return pantryItemRepository.sumQuantityByUserAndProductAndUnitAndStatus(
                USER_ID, product.getId(), Unit.GRAM, PantryItemStatus.ACTIVE);
    }

    private long cookedDishCount() {
        // Scoped to this test's own user: the concurrency test intentionally commits its rows
        // for real (it disables the default test-rollback wrapper), so an unscoped count would
        // also pick up that other test class's permanent leftover row.
        return entityManager.createQuery(
                        "SELECT COUNT(c) FROM CookedDish c WHERE c.userId = :userId", Long.class)
                .setParameter("userId", USER_ID)
                .getSingleResult();
    }
}
