package de.mimosa_dev.MealPlanner.shoppinglist.dto;

import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListItem;

import java.math.BigDecimal;

public record ShoppingListItemResponse(
        Long id, String productName, BigDecimal quantity, String unit, String block, String status) {

    public static ShoppingListItemResponse from(ShoppingListItem item) {
        return new ShoppingListItemResponse(
                item.getId(), item.getProduct().getCanonicalName(), item.getQuantity(), item.getUnit().name(),
                item.getBlock().name(), item.getStatus().name());
    }
}
