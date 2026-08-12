package de.mimosa_dev.MealPlanner.recipe.dto;

import de.mimosa_dev.MealPlanner.recipe.RejectionReason;
import jakarta.validation.constraints.NotNull;

public record RejectSuggestionRequest(@NotNull RejectionReason reason) {
}
