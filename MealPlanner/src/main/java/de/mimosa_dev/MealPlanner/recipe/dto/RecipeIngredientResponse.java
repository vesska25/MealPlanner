package de.mimosa_dev.MealPlanner.recipe.dto;

import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;

import java.math.BigDecimal;

public record RecipeIngredientResponse(String productName, BigDecimal quantity, String unit) {

    public static RecipeIngredientResponse from(RecipeIngredientEntity ingredient) {
        return new RecipeIngredientResponse(
                ingredient.getProduct().getCanonicalName(), ingredient.getQuantity(), ingredient.getUnit().name());
    }
}
