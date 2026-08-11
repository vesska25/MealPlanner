package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import de.mimosa_dev.MealPlanner.recipe.RecipeCandidateScorer;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.recipe.RecipeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import({
        ProposeRecipeCandidatesTool.class, RecipeValidator.class, RecipeCandidateScorer.class,
        ProductNormalizationService.class, PantryService.class, RecipeSuggestionService.class
})
class ProposeRecipeCandidatesToolTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private ProposeRecipeCandidatesTool tool;

    @Autowired
    private ProductNormalizationService normalizationService;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private RecipeRepository recipeRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void aFeasibleCandidateIsAcceptedScoredAndPersisted() {
        givePantryStock("milk", new BigDecimal("500"));

        String result = tool.execute(USER_ID, candidatesInput(List.of(
                candidate("Warm milk", List.of(ingredient("milk", "200", "GRAM")), Set.of(), 5))));

        assertThat(result).contains("Ranked candidates:").contains("Warm milk").contains("saved as recipe #");
        assertThat(recipeRepository.findAll()).hasSize(1);
        assertThat(recipeRepository.findAll().get(0).getName()).isEqualTo("Warm milk");
    }

    @Test
    void aCandidateMissingRequiredEquipmentIsRejectedAndNotPersisted() {
        String result = tool.execute(USER_ID, candidatesInput(List.of(
                candidate("Sous vide egg", List.of(ingredient("egg", "100", "GRAM")),
                        Set.of("sous vide machine"), 30))));

        assertThat(result).contains("Rejected:").contains("Sous vide egg");
        assertThat(recipeRepository.findAll()).isEmpty();
    }

    @Test
    void multipleCandidatesAreRankedWithTheFasterRecipeScoringHigher() {
        givePantryStock("milk", new BigDecimal("1000"));

        String result = tool.execute(USER_ID, candidatesInput(List.of(
                candidate("Slow milk dish", List.of(ingredient("milk", "200", "GRAM")), Set.of(), 55),
                candidate("Quick milk dish", List.of(ingredient("milk", "200", "GRAM")), Set.of(), 5))));

        assertThat(result.indexOf("Quick milk dish")).isLessThan(result.indexOf("Slow milk dish"));
        assertThat(recipeRepository.findAll()).hasSize(2);
    }

    @Test
    void anUnrecognizedProductNameIsResolvedToANewUnverifiedProductRatherThanFailing() {
        // not in the seed catalogue at all — resolving it must create a new unverified Product
        // rather than blowing up; give it pantry stock under the same name so validation passes
        givePantryStock("dragon fruit compote", new BigDecimal("100"));

        String result = tool.execute(USER_ID, candidatesInput(List.of(
                candidate("Mystery bake", List.of(ingredient("dragon fruit compote", "50", "GRAM")),
                        Set.of(), 20))));

        assertThat(result).contains("Ranked candidates:").contains("Mystery bake");
        assertThat(recipeRepository.findAll()).hasSize(1);
    }

    @Test
    void noCandidatesSubmittedIsReportedWithoutPersistingAnything() {
        String result = tool.execute(USER_ID, candidatesInput(List.of()));

        assertThat(result).isEqualTo("No candidates were submitted.");
        assertThat(recipeRepository.findAll()).isEmpty();
    }

    @Test
    void toolDefinitionIsWellFormed() {
        assertThat(tool.definition().name()).isEqualTo(tool.name());
        assertThat(tool.definition().description()).isPresent();
    }

    private void givePantryStock(String canonicalName, BigDecimal quantity) {
        Product product = normalizationService.resolve(canonicalName);
        pantryService.addStock(USER_ID, product, quantity, Unit.GRAM, LocalDate.now().plusDays(30));
    }

    private static JsonValue candidatesInput(List<Map<String, Object>> candidates) {
        return JsonValue.from(Map.of("candidates", candidates));
    }

    private static Map<String, Object> candidate(
            String name, List<Map<String, Object>> ingredients, Set<String> requiredEquipment, int cookTimeMinutes) {
        return Map.of(
                "name", name,
                "ingredients", ingredients,
                "requiredEquipment", requiredEquipment,
                "cookTimeMinutes", cookTimeMinutes,
                "basePortions", 4);
    }

    private static Map<String, Object> ingredient(String productName, String quantity, String unit) {
        return Map.of("productName", productName, "quantity", new BigDecimal(quantity), "unit", unit);
    }
}
