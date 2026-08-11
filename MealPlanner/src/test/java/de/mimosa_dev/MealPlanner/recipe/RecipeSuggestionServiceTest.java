package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(RecipeSuggestionService.class)
class RecipeSuggestionServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private RecipeSuggestionService recipeSuggestionService;

    @Autowired
    private RecipeSuggestionRepository recipeSuggestionRepository;

    @Autowired
    private PreferenceSignalRepository preferenceSignalRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void activatingANewSuggestionExpiresThePreviouslyActiveOne() {
        Long firstRecipeId = savedRecipe("Omelette").getId();
        Long secondRecipeId = savedRecipe("Stir fry").getId();

        recipeSuggestionService.activate(USER_ID, firstRecipeId, new BigDecimal("0.5"));
        entityManager.flush();
        entityManager.clear();
        recipeSuggestionService.activate(USER_ID, secondRecipeId, new BigDecimal("0.7"));

        RecipeSuggestion active = recipeSuggestionRepository
                .findByUserIdAndStatus(USER_ID, RecipeSuggestionStatus.ACTIVE).orElseThrow();
        assertThat(active.getRecipeId()).isEqualTo(secondRecipeId);

        RecipeSuggestion first = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(USER_ID, firstRecipeId, RecipeSuggestionStatus.EXPIRED).orElseThrow();
        assertThat(first.getStatus()).isEqualTo(RecipeSuggestionStatus.EXPIRED);
    }

    @Test
    void rejectingTheActiveSuggestionRecordsAPreferenceSignal() {
        Recipe recipe = savedRecipe("Lentil soup");
        recipeSuggestionService.activate(USER_ID, recipe.getId(), new BigDecimal("0.6"));
        entityManager.flush();
        entityManager.clear();

        recipeSuggestionService.reject(USER_ID, recipe.getId(), RejectionReason.DISLIKE_DISH);

        RecipeSuggestion rejected = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(USER_ID, recipe.getId(), RecipeSuggestionStatus.REJECTED).orElseThrow();
        assertThat(rejected.getRejectionReason()).isEqualTo(RejectionReason.DISLIKE_DISH);
        assertThat(preferenceSignalRepository.existsByUserIdAndRecipeNameIgnoreCase(USER_ID, "Lentil soup")).isTrue();
    }

    @Test
    void rejectingWithNotTodayDoesNotRecordAPreferenceSignal() {
        Recipe recipe = savedRecipe("Pasta bake");
        recipeSuggestionService.activate(USER_ID, recipe.getId(), new BigDecimal("0.6"));
        entityManager.flush();
        entityManager.clear();

        recipeSuggestionService.reject(USER_ID, recipe.getId(), RejectionReason.NOT_TODAY);

        assertThat(preferenceSignalRepository.existsByUserIdAndRecipeNameIgnoreCase(USER_ID, "Pasta bake")).isFalse();
        RecipeSuggestion rejected = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(USER_ID, recipe.getId(), RecipeSuggestionStatus.REJECTED).orElseThrow();
        assertThat(rejected.getRejectionReason()).isEqualTo(RejectionReason.NOT_TODAY);
    }

    @Test
    void rejectingARecipeThatIsNotTheActiveSuggestionFails() {
        Recipe recipe = savedRecipe("Unrelated dish");

        assertThatThrownBy(() -> recipeSuggestionService.reject(USER_ID, recipe.getId(), RejectionReason.DISLIKE_DISH))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void acceptingAMatchingActiveSuggestionMarksItAccepted() {
        Recipe recipe = savedRecipe("Chili");
        recipeSuggestionService.activate(USER_ID, recipe.getId(), new BigDecimal("0.6"));
        entityManager.flush();
        entityManager.clear();

        recipeSuggestionService.accept(USER_ID, recipe.getId());

        RecipeSuggestion accepted = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(USER_ID, recipe.getId(), RecipeSuggestionStatus.ACCEPTED).orElseThrow();
        assertThat(accepted.getStatus()).isEqualTo(RecipeSuggestionStatus.ACCEPTED);
    }

    @Test
    void acceptingWithNoMatchingActiveSuggestionIsANoOp() {
        Recipe recipe = savedRecipe("Never suggested");

        recipeSuggestionService.accept(USER_ID, recipe.getId());

        assertThat(recipeSuggestionRepository.findByUserIdAndStatus(USER_ID, RecipeSuggestionStatus.ACTIVE)).isEmpty();
    }

    private Recipe savedRecipe(String name) {
        return recipeRepository.save(new Recipe(USER_ID, name, 20, 2, Set.of()));
    }
}
