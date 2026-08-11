package de.mimosa_dev.MealPlanner.cooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * {@code idempotencyKey} is client-generated (AI-15a: "an idempotency key passed explicitly at
 * the API level") — a retry after a dropped connection resends the same key rather than the
 * server minting a new one, so it's safe by construction.
 */
public record ConfirmCookingRequest(
        @NotNull Long recipeId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal actualPortions,
        @NotBlank String idempotencyKey) {
}
