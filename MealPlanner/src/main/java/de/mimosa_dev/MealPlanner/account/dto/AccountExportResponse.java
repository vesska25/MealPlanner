package de.mimosa_dev.MealPlanner.account.dto;

import java.util.List;

/** FR-04: everything a user owns, in one machine-readable response. */
public record AccountExportResponse(
        String email,
        List<PantryItemExport> pantryItems,
        List<RecipeExport> recipes,
        List<CookedDishExport> cookedDishes,
        List<AgentRunExport> agentRuns) {
}
