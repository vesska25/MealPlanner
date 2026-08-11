package de.mimosa_dev.MealPlanner.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {
}
