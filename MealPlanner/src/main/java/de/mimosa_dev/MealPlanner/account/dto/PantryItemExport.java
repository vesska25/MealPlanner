package de.mimosa_dev.MealPlanner.account.dto;

import de.mimosa_dev.MealPlanner.common.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PantryItemExport(
        Long id, String productName, BigDecimal quantity, Unit unit,
        LocalDate purchasedAt, LocalDate expiresAt, String status, String discardReason) {
}
