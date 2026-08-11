package de.mimosa_dev.MealPlanner.recipe.dto;

import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ActiveSuggestionResponse(
        Long suggestionId, Long recipeId, String recipeName, Integer cookTimeMinutes, Integer basePortions,
        Set<String> requiredEquipment, List<RecipeIngredientResponse> ingredients, BigDecimal score) {

    public static ActiveSuggestionResponse from(RecipeSuggestion suggestion, Recipe recipe) {
        return new ActiveSuggestionResponse(
                suggestion.getId(), recipe.getId(), recipe.getName(), recipe.getCookTimeMinutes(), recipe.getBasePortions(),
                recipe.getRequiredEquipment(),
                recipe.getIngredients().stream().map(RecipeIngredientResponse::from).toList(),
                suggestion.getScore());
    }
}
