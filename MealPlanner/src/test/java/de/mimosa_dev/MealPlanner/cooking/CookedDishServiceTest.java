package de.mimosa_dev.MealPlanner.cooking;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.recipe.DishCategory;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(CookedDishService.class)
class CookedDishServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private CookedDishService cookedDishService;

    @Autowired
    private CookedDishRepository cookedDishRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void consumingPartialPortionsDecrementsTheRemainderAndStaysActive() {
        CookedDish dish = savedCookedDish("4");

        cookedDishService.consumePortions(USER_ID, dish.getId(), new BigDecimal("1.5"));

        CookedDish reloaded = cookedDishRepository.findById(dish.getId()).orElseThrow();
        assertThat(reloaded.getPortionsRemaining()).isEqualByComparingTo("2.5");
        assertThat(reloaded.getStatus()).isEqualTo(CookedDishStatus.ACTIVE);
    }

    @Test
    void consumingTheLastPortionFlipsStatusToConsumed() {
        CookedDish dish = savedCookedDish("2");

        cookedDishService.consumePortions(USER_ID, dish.getId(), new BigDecimal("2"));

        CookedDish reloaded = cookedDishRepository.findById(dish.getId()).orElseThrow();
        assertThat(reloaded.getPortionsRemaining()).isEqualByComparingTo("0");
        assertThat(reloaded.getStatus()).isEqualTo(CookedDishStatus.CONSUMED);
    }

    @Test
    void consumingMoreThanRemainingIsRejected() {
        CookedDish dish = savedCookedDish("2");

        assertThatThrownBy(() -> cookedDishService.consumePortions(USER_ID, dish.getId(), new BigDecimal("3")))
                .isInstanceOf(InsufficientPortionsException.class);
        assertThat(cookedDishRepository.findById(dish.getId()).orElseThrow().getPortionsRemaining())
                .isEqualByComparingTo("2");
    }

    @Test
    void aConsumedDishCannotBeConsumedAgain() {
        CookedDish dish = savedCookedDish("1");
        cookedDishService.consumePortions(USER_ID, dish.getId(), new BigDecimal("1"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> cookedDishService.consumePortions(USER_ID, dish.getId(), new BigDecimal("0.1")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void discardMarksAnActiveDishDiscarded() {
        CookedDish dish = savedCookedDish("3");

        cookedDishService.discard(USER_ID, dish.getId());

        assertThat(cookedDishRepository.findById(dish.getId()).orElseThrow().getStatus())
                .isEqualTo(CookedDishStatus.DISCARDED);
    }

    @Test
    void aDiscardedDishCannotBeDiscardedAgain() {
        CookedDish dish = savedCookedDish("3");
        cookedDishService.discard(USER_ID, dish.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> cookedDishService.discard(USER_ID, dish.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void consumingAnotherUsersDishIsTreatedAsNotFound() {
        CookedDish dish = savedCookedDish("2");
        entityManager.flush();
        entityManager.clear();

        Long someoneElse = USER_ID + 1;
        assertThatThrownBy(() -> cookedDishService.consumePortions(someoneElse, dish.getId(), new BigDecimal("1")))
                .isInstanceOf(NoSuchElementException.class);
    }

    private CookedDish savedCookedDish(String totalPortions) {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(USER_ID, "Test recipe", 20, 1, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("200"), Unit.GRAM));
        recipe = recipeRepository.save(recipe);

        LocalDate today = LocalDate.now();
        CookedDish dish = new CookedDish(
                USER_ID, recipe, DishCategory.UNKNOWN, new BigDecimal(totalPortions),
                null, null, null, null, today, today.plusDays(2), "key-" + System.nanoTime());
        return cookedDishRepository.save(dish);
    }
}
