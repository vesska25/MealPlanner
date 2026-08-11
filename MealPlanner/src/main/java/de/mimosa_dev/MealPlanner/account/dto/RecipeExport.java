package de.mimosa_dev.MealPlanner.account.dto;

import java.util.List;
import java.util.Set;

public record RecipeExport(
        Long id, String name, Integer cookTimeMinutes, Integer basePortions,
        Set<String> requiredEquipment, List<RecipeIngredientExport> ingredients) {
}
