package de.mimosa_dev.MealPlanner.pantry.dto;

import de.mimosa_dev.MealPlanner.pantry.PantryItem;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PantryItemResponse(
        Long id, String productName, BigDecimal quantity, String unit,
        LocalDate purchasedAt, LocalDate expiresAt, boolean estimated) {

    public static PantryItemResponse from(PantryItem item) {
        return new PantryItemResponse(
                item.getId(), item.getProduct().getCanonicalName(), item.getQuantity(), item.getUnit().name(),
                item.getPurchasedAt(), item.getExpiresAt(), item.isEstimated());
    }
}
