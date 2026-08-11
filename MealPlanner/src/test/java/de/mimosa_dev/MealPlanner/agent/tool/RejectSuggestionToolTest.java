package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import({RejectSuggestionTool.class, RecipeSuggestionService.class})
class RejectSuggestionToolTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private RejectSuggestionTool tool;

    @Autowired
    private RecipeSuggestionService recipeSuggestionService;

    @Autowired
    private RecipeSuggestionRepository recipeSuggestionRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void rejectingTheActiveSuggestionFlipsItsStatus() {
        Recipe recipe = recipeRepository.save(new Recipe(USER_ID, "Bean chili", 20, 2, Set.of()));
        recipeSuggestionService.activate(USER_ID, recipe.getId(), new BigDecimal("0.6"));
        entityManager.flush();
        entityManager.clear();

        String result = tool.execute(USER_ID, JsonValue.from(Map.of(
                "recipeId", recipe.getId(), "reason", "TAKES_TOO_LONG")));

        assertThat(result).contains("Rejected");
        assertThat(recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(USER_ID, recipe.getId(), RecipeSuggestionStatus.REJECTED))
                .isPresent();
    }

    @Test
    void toolDefinitionIsWellFormed() {
        assertThat(tool.definition().name()).isEqualTo(tool.name());
        assertThat(tool.definition().description()).isPresent();
    }
}
