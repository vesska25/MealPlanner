package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Import({RecipeCandidateScorer.class, PantryService.class})
class RecipeCandidateScorerTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private RecipeCandidateScorer scorer;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PantryService pantryService;

    @Test
    void aRecipeUsingAnIngredientExpiringSoonScoresHigherThanOneExpiringFar() {
        Product milk = seededProduct("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now().plusDays(1));

        Product rice = seededProduct("rice");
        pantryService.addStock(USER_ID, rice, new BigDecimal("500"), Unit.GRAM, LocalDate.now().plusDays(60));

        RecipeCandidate soonToExpire = recipeOf(milk, new BigDecimal("200"), 20);
        RecipeCandidate notExpiringSoon = recipeOf(rice, new BigDecimal("200"), 20);

        assertThat(scorer.score(USER_ID, soonToExpire)).isGreaterThan(scorer.score(USER_ID, notExpiringSoon));
    }

    @Test
    void aFullyCoveredRecipeScoresHigherThanOneMissingStock() {
        Product milk = seededProduct("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("1000"), Unit.GRAM, LocalDate.now().plusDays(30));

        Product rice = seededProduct("rice");
        pantryService.addStock(USER_ID, rice, new BigDecimal("50"), Unit.GRAM, LocalDate.now().plusDays(30));

        RecipeCandidate covered = recipeOf(milk, new BigDecimal("200"), 20);
        RecipeCandidate uncovered = recipeOf(rice, new BigDecimal("500"), 20);

        assertThat(scorer.score(USER_ID, covered)).isGreaterThan(scorer.score(USER_ID, uncovered));
    }

    @Test
    void aQuickerRecipeScoresHigherThanAnOtherwiseIdenticalSlowerOne() {
        Product milk = seededProduct("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("1000"), Unit.GRAM, LocalDate.now().plusDays(30));

        RecipeCandidate quick = recipeOf(milk, new BigDecimal("200"), 10);
        RecipeCandidate slow = recipeOf(milk, new BigDecimal("200"), 55);

        assertThat(scorer.score(USER_ID, quick)).isGreaterThan(scorer.score(USER_ID, slow));
    }

    @Test
    void staplesAreIgnoredForExpiryUrgencyAndCountAsFullyCovered() {
        Product salt = seededProduct("salt"); // is_staple = true, no pantry stock given at all

        RecipeCandidate saltOnly = recipeOf(salt, new BigDecimal("5"), 20);

        // no expiry urgency (staples are skipped) and full coverage (an all-staple recipe has
        // no trackable ingredients left, which counts as fully covered)
        double expected = 0.30 * 1.0 + 0.15 * (1.0 - 20.0 / 60.0) + 0.05 * 0.5;
        assertThat(scorer.score(USER_ID, saltOnly)).isCloseTo(expected, within(0.001));
    }

    @Test
    void aRecipeWithNoIngredientsIsFullyCoveredWithNoUrgency() {
        RecipeCandidate noIngredients = new RecipeCandidate("Just boil water", List.of(), Set.of(), 5);

        double expected = 0.30 * 1.0 + 0.15 * (1.0 - 5.0 / 60.0) + 0.05 * 0.5;
        assertThat(scorer.score(USER_ID, noIngredients)).isCloseTo(expected, within(0.001));
    }

    private Product seededProduct(String canonicalName) {
        return productRepository.findByCanonicalNameIgnoreCase(canonicalName).orElseThrow();
    }

    private static RecipeCandidate recipeOf(Product product, BigDecimal quantity, int cookTimeMinutes) {
        return new RecipeCandidate(
                "Test recipe",
                List.of(new RecipeIngredient(product.getId(), quantity, Unit.GRAM)),
                Set.of(),
                cookTimeMinutes);
    }
}
