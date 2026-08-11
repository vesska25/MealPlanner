package de.mimosa_dev.MealPlanner.recipe;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * AI-20a/b/c: when the meal-planning agent run exhausts its iteration budget, fall back to a
 * deterministic SQL-driven pick from the user's own previously-saved recipes instead of an
 * empty screen or an error. AI-20b requires the fallback to respect every hard constraint the
 * normal flow does — reusing {@link RecipeValidator} (current pantry, current constraints, and
 * FR-33's recency exclusion) rather than a separate, looser check is what guarantees that.
 *
 * <p>The candidate pool is every recipe ever validated for this user ({@link
 * RecipeRepository#findByUserId}), not narrowed to ones actually accepted or cooked — a tighter
 * pool would be a better fallback but needs a new cross-entity query for a path that's already
 * a last resort. Flagged as a reasonable future refinement, not required now.
 */
@Service
public class MealPlanningFallbackService {

    private final RecipeRepository recipeRepository;
    private final RecipeValidator recipeValidator;
    private final RecipeCandidateScorer scorer;

    public MealPlanningFallbackService(
            RecipeRepository recipeRepository, RecipeValidator recipeValidator, RecipeCandidateScorer scorer) {
        this.recipeRepository = recipeRepository;
        this.recipeValidator = recipeValidator;
        this.scorer = scorer;
    }

    public record FallbackPick(Recipe recipe, double score) {
    }

    public Optional<FallbackPick> selectFallback(Long userId) {
        List<Recipe> ownRecipes = recipeRepository.findByUserId(userId).stream()
                .map(recipe -> recipeRepository.findWithIngredientsById(recipe.getId()).orElseThrow())
                .toList();

        UserConstraints constraints = UserConstraints.defaults();
        return ownRecipes.stream()
                .filter(recipe -> recipeValidator.validate(userId, RecipeCandidate.fromRecipe(recipe), constraints).valid())
                .map(recipe -> new FallbackPick(recipe, scorer.score(userId, RecipeCandidate.fromRecipe(recipe))))
                .max(Comparator.comparingDouble(FallbackPick::score));
    }
}
