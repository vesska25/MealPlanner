package de.mimosa_dev.MealPlanner.recipe.controller;

import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestion;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionStatus;
import de.mimosa_dev.MealPlanner.recipe.dto.ActiveSuggestionResponse;
import de.mimosa_dev.MealPlanner.recipe.dto.RejectSuggestionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * "What's cooking" view plus accept/reject actions (PRD 9.1 step 11). Accept/reject have no
 * agent-tool equivalent for accept (it's normally a side effect of {@code CookingService
 * .confirmCooking}) and only a chat-message path for reject ({@code RejectSuggestionTool}) — these
 * REST actions wrap the same {@link RecipeSuggestionService} methods directly, giving the web
 * client real buttons instead of round-tripping through the chat scenario.
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeSuggestionRepository recipeSuggestionRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeSuggestionService recipeSuggestionService;

    public RecipeController(
            RecipeSuggestionRepository recipeSuggestionRepository,
            RecipeRepository recipeRepository,
            RecipeSuggestionService recipeSuggestionService) {
        this.recipeSuggestionRepository = recipeSuggestionRepository;
        this.recipeRepository = recipeRepository;
        this.recipeSuggestionService = recipeSuggestionService;
    }

    @GetMapping("/suggestions/active")
    public ActiveSuggestionResponse activeSuggestion(@AuthenticationPrincipal Long userId) {
        RecipeSuggestion suggestion = recipeSuggestionRepository
                .findByUserIdAndStatus(userId, RecipeSuggestionStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active suggestion"));
        Recipe recipe = recipeRepository.findWithIngredientsById(suggestion.getRecipeId())
                .orElseThrow(() -> new NoSuchElementException("Recipe " + suggestion.getRecipeId() + " not found"));
        return ActiveSuggestionResponse.from(suggestion, recipe);
    }

    // RecipeSuggestionService.accept() is a deliberately forgiving no-op when nothing is active
    // (it doubles as CookingService's side-effect call) — a real user clicking "Accept" on a
    // specific suggestion expects a 404 if it's no longer there, so that existence check happens
    // here rather than relying on the service's own (silent) behavior.
    @PostMapping("/suggestions/{recipeId}/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal Long userId, @PathVariable Long recipeId) {
        recipeSuggestionRepository.findByUserIdAndRecipeIdAndStatus(userId, recipeId, RecipeSuggestionStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("No active suggestion for recipe " + recipeId));
        recipeSuggestionService.accept(userId, recipeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/suggestions/{recipeId}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal Long userId, @PathVariable Long recipeId,
            @Valid @RequestBody RejectSuggestionRequest request) {
        recipeSuggestionService.reject(userId, recipeId, request.reason());
        return ResponseEntity.noContent().build();
    }
}
