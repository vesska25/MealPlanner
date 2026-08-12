package de.mimosa_dev.MealPlanner.pantry.dto;

import de.mimosa_dev.MealPlanner.pantry.DiscardReason;
import jakarta.validation.constraints.NotNull;

public record DiscardPantryItemRequest(@NotNull DiscardReason reason) {
}
