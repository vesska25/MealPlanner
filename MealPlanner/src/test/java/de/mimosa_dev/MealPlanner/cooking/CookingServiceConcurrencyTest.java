package de.mimosa_dev.MealPlanner.cooking;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.mealentry.MealEntryService;
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
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves NFR-43b actually does something, rather than trusting the {@code @Lock} annotation on
 * faith: two cooking confirmations race against stock that only covers one of them. Without the
 * pessimistic lock, both could read "sufficient" before either deducts. Runs each test method
 * non-transactionally ({@code NOT_SUPPORTED} overrides {@code @DataJpaTest}'s default rollback
 * wrapper) so the setup data is actually committed and visible to the two concurrently-running
 * transactions spawned below — a thread-per-call doesn't join the test method's own transaction.
 */
@Import({
        CookingService.class, DishCategoryResolver.class, NutritionCalculationService.class,
        PantryService.class, RecipeSuggestionService.class, MealEntryService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CookingServiceConcurrencyTest extends AbstractIntegrationTest {

    private static final Long USER_ID = -100L; // distinct from other tests' fixed user ids

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
    private CookedDishRepository cookedDishRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void exactlyOneOfTwoConcurrentConfirmationsSucceedsWhenStockOnlyCoversOne() throws Exception {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        pantryService.addStock(USER_ID, milk, new BigDecimal("200"), Unit.GRAM, LocalDate.now());
        Recipe recipe = new Recipe(USER_ID, "Concurrency test recipe", 10, 1, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("200"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> attemptA = confirmAttempt(saved.getId(), "concurrency-key-A", ready, go);
            Callable<Object> attemptB = confirmAttempt(saved.getId(), "concurrency-key-B", ready, go);

            Future<Object> futureA = executor.submit(attemptA);
            Future<Object> futureB = executor.submit(attemptB);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<Object> results = List.of(futureA.get(10, TimeUnit.SECONDS), futureB.get(10, TimeUnit.SECONDS));

            long successes = results.stream().filter(r -> r instanceof CookedDish).count();
            long rejections = results.stream().filter(r -> r instanceof CookingInfeasibleException).count();
            assertThat(successes).isEqualTo(1);
            assertThat(rejections).isEqualTo(1);
        } finally {
            executor.shutdown();
        }

        BigDecimal remainingStock = pantryItemRepository.sumQuantityByUserAndProductAndUnitAndStatus(
                USER_ID, milk.getId(), Unit.GRAM, PantryItemStatus.ACTIVE);
        assertThat(remainingStock).isEqualByComparingTo("0"); // never negative, deducted exactly once
        long cookedDishesForUser = cookedDishRepository.findAll().stream()
                .filter(dish -> dish.getUserId().equals(USER_ID)).count();
        assertThat(cookedDishesForUser).isEqualTo(1);
    }

    private Callable<Object> confirmAttempt(Long recipeId, String idempotencyKey, CountDownLatch ready, CountDownLatch go) {
        return () -> {
            ready.countDown();
            go.await();
            try {
                return cookingService.confirmCooking(USER_ID, recipeId, BigDecimal.ONE, idempotencyKey);
            } catch (CookingInfeasibleException e) {
                return e;
            }
        };
    }
}
