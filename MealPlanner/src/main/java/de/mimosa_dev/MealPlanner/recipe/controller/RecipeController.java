package de.mimosa_dev.MealPlanner.recipe.controller;

import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestion;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionStatus;
import de.mimosa_dev.MealPlanner.recipe.dto.ActiveSuggestionResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/** Read-only "what's cooking" view (PRD 9.1 step 11). */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeSuggestionRepository recipeSuggestionRepository;
    private final RecipeRepository recipeRepository;

    public RecipeController(RecipeSuggestionRepository recipeSuggestionRepository, RecipeRepository recipeRepository) {
        this.recipeSuggestionRepository = recipeSuggestionRepository;
        this.recipeRepository = recipeRepository;
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
}
