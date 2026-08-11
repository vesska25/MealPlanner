package de.mimosa_dev.MealPlanner.shoppinglist;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({ShoppingListService.class, RecipeSuggestionService.class, PantryService.class, ProductNormalizationService.class})
class ShoppingListServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private ShoppingListService shoppingListService;

    @Autowired
    private RecipeSuggestionService recipeSuggestionService;

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

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void missingNonStapleIngredientLandsInDefinitelyNeed() {
        Product milk = seeded("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("100"), Unit.GRAM, LocalDate.now());
        activateSuggestionFor("Milk soup", ingredient(milk, "300"));
        entityManager.flush();
        entityManager.clear();

        var result = shoppingListService.generate(USER_ID);

        assertThat(result.shoppingList().getItems()).hasSize(1);
        ShoppingListItem item = result.shoppingList().getItems().get(0);
        assertThat(item.getBlock()).isEqualTo(ShoppingListBlock.DEFINITELY_NEED);
        assertThat(item.getQuantity()).isEqualByComparingTo("200"); // 300 needed - 100 in stock
    }

    @Test
    void sufficientlyStockedIngredientProducesNoItem() {
        Product milk = seeded("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("1000"), Unit.GRAM, LocalDate.now());
        activateSuggestionFor("Milk soup", ingredient(milk, "300"));
        entityManager.flush();
        entityManager.clear();

        var result = shoppingListService.generate(USER_ID);

        assertThat(result.shoppingList().getItems()).isEmpty();
    }

    @Test
    void stapleIngredientLandsInCheckMaybeOutAtTheRecipesQuantity() {
        Product salt = seeded("salt"); // is_staple = true
        activateSuggestionFor("Salted broth", ingredient(salt, "10"));
        entityManager.flush();
        entityManager.clear();

        var result = shoppingListService.generate(USER_ID);

        assertThat(result.shoppingList().getItems()).hasSize(1);
        ShoppingListItem item = result.shoppingList().getItems().get(0);
        assertThat(item.getBlock()).isEqualTo(ShoppingListBlock.CHECK_MAYBE_OUT);
        assertThat(item.getQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void noActiveSuggestionProducesAnEmptyList() {
        var result = shoppingListService.generate(USER_ID);

        assertThat(result.shoppingList().getItems()).isEmpty();
    }

    @Test
    void pantryIsRunningLowWhenThereIsNoActiveNonStapleStock() {
        var result = shoppingListService.generate(USER_ID);

        assertThat(result.pantryRunningLow()).isTrue();
    }

    @Test
    void pantryIsNotRunningLowWhenSomethingExpiresWellInTheFuture() {
        Product milk = seeded("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now().plusDays(1));
        // milk's own default shelf life pushes expiry well past the 2-day low-stock horizon in
        // the seed data (7 days) — sanity-checked indirectly via the assertion below.

        var result = shoppingListService.generate(USER_ID);

        assertThat(result.pantryRunningLow()).isFalse();
    }

    @Test
    void resolvingPurchasedAddsEstimatedStockToThePantry() {
        Product milk = seeded("milk");
        activateSuggestionFor("Milk soup", ingredient(milk, "300"));
        entityManager.flush();
        entityManager.clear();
        Long itemId = shoppingListService.generate(USER_ID).shoppingList().getItems().get(0).getId();
        entityManager.flush();
        entityManager.clear();

        shoppingListService.resolveItems(USER_ID,
                List.of(new ShoppingListService.ItemResolution(itemId, ShoppingListItemStatus.PURCHASED)),
                List.of());

        var pantryItems = pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(USER_ID, PantryItemStatus.ACTIVE);
        assertThat(pantryItems).hasSize(1);
        assertThat(pantryItems.get(0).getQuantity()).isEqualByComparingTo("300");
        assertThat(pantryItems.get(0).isEstimated()).isTrue();
    }

    @Test
    void resolvingNotBuyingDoesNotTouchThePantry() {
        Product milk = seeded("milk");
        activateSuggestionFor("Milk soup", ingredient(milk, "300"));
        entityManager.flush();
        entityManager.clear();
        Long itemId = shoppingListService.generate(USER_ID).shoppingList().getItems().get(0).getId();
        entityManager.flush();
        entityManager.clear();

        shoppingListService.resolveItems(USER_ID,
                List.of(new ShoppingListService.ItemResolution(itemId, ShoppingListItemStatus.NOT_BUYING)),
                List.of());

        assertThat(pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(USER_ID, PantryItemStatus.ACTIVE)).isEmpty();
    }

    @Test
    void extraPurchasesAddNonEstimatedStock() {
        shoppingListService.resolveItems(USER_ID, List.of(),
                List.of(new ShoppingListService.ExtraPurchase("eggs", new BigDecimal("6"), Unit.PIECE)));

        var pantryItems = pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(USER_ID, PantryItemStatus.ACTIVE);
        assertThat(pantryItems).hasSize(1);
        assertThat(pantryItems.get(0).isEstimated()).isFalse();
    }

    @Test
    void resolvingAnotherUsersItemFails() {
        Product milk = seeded("milk");
        activateSuggestionFor("Milk soup", ingredient(milk, "300"));
        entityManager.flush();
        entityManager.clear();
        Long itemId = shoppingListService.generate(USER_ID).shoppingList().getItems().get(0).getId();
        entityManager.flush();
        entityManager.clear();

        Long someoneElse = USER_ID + 1;
        assertThatThrownBy(() -> shoppingListService.resolveItems(someoneElse,
                List.of(new ShoppingListService.ItemResolution(itemId, ShoppingListItemStatus.PURCHASED)),
                List.of()))
                .isInstanceOf(NoSuchElementException.class);
    }

    private Product seeded(String canonicalName) {
        return productRepository.findByCanonicalNameIgnoreCase(canonicalName).orElseThrow();
    }

    private RecipeIngredientEntity ingredient(Product product, String quantity) {
        return new RecipeIngredientEntity(product, new BigDecimal(quantity), Unit.GRAM);
    }

    private void activateSuggestionFor(String recipeName, RecipeIngredientEntity ingredient) {
        Recipe recipe = new Recipe(USER_ID, recipeName, 20, 2, Set.of());
        recipe.addIngredient(ingredient);
        Recipe saved = recipeRepository.save(recipe);
        recipeSuggestionService.activate(USER_ID, saved.getId(), new BigDecimal("0.5"));
    }
}
