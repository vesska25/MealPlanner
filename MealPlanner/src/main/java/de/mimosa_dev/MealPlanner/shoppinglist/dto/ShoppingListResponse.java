package de.mimosa_dev.MealPlanner.shoppinglist.dto;

import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingList;

import java.time.Instant;
import java.util.List;

public record ShoppingListResponse(Long id, Instant createdAt, List<ShoppingListItemResponse> items) {

    public static ShoppingListResponse from(ShoppingList list) {
        return new ShoppingListResponse(
                list.getId(), list.getCreatedAt(),
                list.getItems().stream().map(ShoppingListItemResponse::from).toList());
    }
}
