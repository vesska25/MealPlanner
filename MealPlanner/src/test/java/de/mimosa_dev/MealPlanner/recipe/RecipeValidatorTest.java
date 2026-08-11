package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.cooking.CookedDish;
import de.mimosa_dev.MealPlanner.cooking.CookedDishRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import({RecipeValidator.class, PantryService.class})
class RecipeValidatorTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private RecipeValidator recipeValidator;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private CookedDishRepository cookedDishRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void passesWhenAllConstraintsAreSatisfied() {
        Product milk = seededProduct("milk");
        Product salt = seededProduct("salt"); // is_staple = true
        givePantryStock(milk, new BigDecimal("500"));

        RecipeCandidate recipe = new RecipeCandidate(
                "Salted milk",
                List.of(
                        new RecipeIngredient(milk.getId(), new BigDecimal("200"), Unit.GRAM),
                        new RecipeIngredient(salt.getId(), new BigDecimal("5"), Unit.GRAM)),
                Set.of("stove"),
                15);
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of("stove", "oven"), 30);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.valid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void rejectsAnAllergenicIngredientEvenWithEnoughStock() {
        Product milk = seededProduct("milk");
        givePantryStock(milk, new BigDecimal("500"));

        RecipeCandidate recipe = recipeOf(new RecipeIngredient(milk.getId(), new BigDecimal("200"), Unit.GRAM));
        UserConstraints constraints = new UserConstraints(Set.of(milk.getId()), Set.of(), 60);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations())
                .extracting(RecipeViolation::type)
                .containsExactly(RecipeViolationType.ALLERGEN_OR_EXCLUDED_INGREDIENT);
    }

    @Test
    void flagsInsufficientStockForANonStapleIngredient() {
        Product milk = seededProduct("milk");
        givePantryStock(milk, new BigDecimal("100"));

        RecipeCandidate recipe = recipeOf(new RecipeIngredient(milk.getId(), new BigDecimal("500"), Unit.GRAM));
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of(), 60);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.violations())
                .extracting(RecipeViolation::type)
                .containsExactly(RecipeViolationType.INSUFFICIENT_PANTRY_STOCK);
    }

    @Test
    void staplesAreAssumedAvailableRegardlessOfPantryContents() {
        Product salt = seededProduct("salt"); // no pantry stock given at all

        RecipeCandidate recipe = recipeOf(new RecipeIngredient(salt.getId(), new BigDecimal("5"), Unit.GRAM));
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of(), 60);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void flagsMissingEquipmentCaseInsensitively() {
        RecipeCandidate recipe = new RecipeCandidate("Roast", List.of(), Set.of("Oven"), 45);
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of("stove"), 60);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.violations())
                .extracting(RecipeViolation::type)
                .containsExactly(RecipeViolationType.MISSING_EQUIPMENT);
    }

    @Test
    void flagsCookTimeOverTheLimit() {
        RecipeCandidate recipe = new RecipeCandidate("Slow braise", List.of(), Set.of(), 120);
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of(), 30);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.violations())
                .extracting(RecipeViolation::type)
                .containsExactly(RecipeViolationType.COOK_TIME_EXCEEDS_LIMIT);
    }

    @Test
    void flagsAnUnknownProductIdWithoutBlowingUp() {
        RecipeCandidate recipe = recipeOf(new RecipeIngredient(999_999L, new BigDecimal("1"), Unit.GRAM));
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of(), 60);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.violations())
                .extracting(RecipeViolation::type)
                .containsExactly(RecipeViolationType.UNKNOWN_PRODUCT);
    }

    @Test
    void collectsAllViolationsRatherThanFailingFast() {
        Product milk = seededProduct("milk");
        givePantryStock(milk, new BigDecimal("10")); // not enough for the recipe below

        RecipeCandidate recipe = new RecipeCandidate(
                "Everything wrong",
                List.of(new RecipeIngredient(milk.getId(), new BigDecimal("500"), Unit.GRAM)),
                Set.of("sous vide machine"),
                999);
        UserConstraints constraints = new UserConstraints(Set.of(milk.getId()), Set.of(), 10);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.violations())
                .extracting(RecipeViolation::type)
                .containsExactlyInAnyOrder(
                        RecipeViolationType.ALLERGEN_OR_EXCLUDED_INGREDIENT,
                        RecipeViolationType.INSUFFICIENT_PANTRY_STOCK,
                        RecipeViolationType.MISSING_EQUIPMENT,
                        RecipeViolationType.COOK_TIME_EXCEEDS_LIMIT);
    }

    @Test
    void flagsARecipeCookedWithinTheLastFourteenDays() {
        cookDishNamed("Weeknight stir fry", LocalDate.now().minusDays(3));

        RecipeCandidate recipe = new RecipeCandidate("Weeknight stir fry", List.of(), Set.of(), 20);
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of(), 60);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.violations())
                .extracting(RecipeViolation::type)
                .containsExactly(RecipeViolationType.RECENTLY_COOKED);
    }

    @Test
    void doesNotFlagARecipeCookedBeforeTheFourteenDayWindow() {
        cookDishNamed("Old favorite", LocalDate.now().minusDays(20));

        RecipeCandidate recipe = new RecipeCandidate("Old favorite", List.of(), Set.of(), 20);
        UserConstraints constraints = new UserConstraints(Set.of(), Set.of(), 60);

        RecipeValidationResult result = recipeValidator.validate(USER_ID, recipe, constraints);

        assertThat(result.valid()).isTrue();
    }

    private static RecipeCandidate recipeOf(RecipeIngredient ingredient) {
        return new RecipeCandidate("Test recipe", List.of(ingredient), Set.of(), 20);
    }

    private Product seededProduct(String canonicalName) {
        return productRepository.findByCanonicalNameIgnoreCase(canonicalName).orElseThrow();
    }

    private void givePantryStock(Product product, BigDecimal quantity) {
        pantryService.addStock(USER_ID, product, quantity, Unit.GRAM, LocalDate.now());
    }

    private void cookDishNamed(String recipeName, LocalDate cookedAt) {
        Recipe recipe = recipeRepository.save(new Recipe(USER_ID, recipeName, 20, 2, Set.of()));
        CookedDish dish = new CookedDish(
                USER_ID, recipe, DishCategory.UNKNOWN, new BigDecimal("2"),
                null, null, null, null, cookedAt, cookedAt.plusDays(2), "key-" + recipeName + "-" + cookedAt);
        cookedDishRepository.save(dish);
    }
}
