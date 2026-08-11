package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        GenerateShoppingListTool.class, ShoppingListService.class, RecipeSuggestionService.class,
        PantryService.class, ProductNormalizationService.class
})
class GenerateShoppingListToolTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private GenerateShoppingListTool tool;

    @Autowired
    private RecipeSuggestionService recipeSuggestionService;

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
    void generatesAListWithADefinitelyNeedEntryForTheActiveSuggestion() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(USER_ID, "Milk soup", 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("300"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);
        recipeSuggestionService.activate(USER_ID, saved.getId(), new BigDecimal("0.5"));
        entityManager.flush();
        entityManager.clear();

        String result = tool.execute(USER_ID, JsonValue.from(Map.of()));

        assertThat(result).contains("Definitely need").contains("milk");
    }

    @Test
    void reportsNothingToBuyWhenThereIsNoActiveSuggestion() {
        String result = tool.execute(USER_ID, JsonValue.from(Map.of()));

        assertThat(result).contains("Nothing to buy");
    }

    @Test
    void toolDefinitionIsWellFormed() {
        assertThat(tool.definition().name()).isEqualTo(tool.name());
        assertThat(tool.definition().description()).isPresent();
    }
}
