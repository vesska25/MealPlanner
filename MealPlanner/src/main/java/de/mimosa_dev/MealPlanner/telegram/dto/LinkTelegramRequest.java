package de.mimosa_dev.MealPlanner.telegram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LinkTelegramRequest(@NotBlank String code, @NotNull Long telegramUserId) {
}
