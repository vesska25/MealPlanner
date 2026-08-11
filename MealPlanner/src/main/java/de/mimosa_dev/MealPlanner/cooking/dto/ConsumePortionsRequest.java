package de.mimosa_dev.MealPlanner.cooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConsumePortionsRequest(@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal portionsEaten) {
}
