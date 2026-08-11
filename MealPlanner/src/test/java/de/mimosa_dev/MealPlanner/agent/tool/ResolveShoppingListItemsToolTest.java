package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
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
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        ResolveShoppingListItemsTool.class, ShoppingListService.class, RecipeSuggestionService.class,
        PantryService.class, ProductNormalizationService.class
})
class ResolveShoppingListItemsToolTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private ResolveShoppingListItemsTool tool;

    @Autowired
    private ShoppingListService shoppingListService;

    @Autowired
    private RecipeSuggestionService recipeSuggestionService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void resolvingAnItemAsPurchasedAddsEstimatedPantryStock() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(USER_ID, "Milk soup", 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("300"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);
        recipeSuggestionService.activate(USER_ID, saved.getId(), new BigDecimal("0.5"));
        entityManager.flush();
        entityManager.clear();
        Long itemId = shoppingListService.generate(USER_ID).shoppingList().getItems().get(0).getId();
        entityManager.flush();
        entityManager.clear();

        String result = tool.execute(USER_ID, JsonValue.from(Map.of(
                "items", List.of(Map.of("itemId", itemId, "status", "PURCHASED")))));

        assertThat(result).contains("1 item update");
        assertThat(pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(USER_ID, PantryItemStatus.ACTIVE))
                .hasSize(1);
    }

    @Test
    void extraPurchasesAreRecordedInThePantry() {
        String result = tool.execute(USER_ID, JsonValue.from(Map.of(
                "extraPurchases", List.of(Map.of("productName", "eggs", "quantity", 6, "unit", "PIECE")))));

        assertThat(result).contains("1 extra purchase");
        assertThat(pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(USER_ID, PantryItemStatus.ACTIVE))
                .hasSize(1);
    }

    @Test
    void toolDefinitionIsWellFormed() {
        assertThat(tool.definition().name()).isEqualTo(tool.name());
        assertThat(tool.definition().description()).isPresent();
    }
}
