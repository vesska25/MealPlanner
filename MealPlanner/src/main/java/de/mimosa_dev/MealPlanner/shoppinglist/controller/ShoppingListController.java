package de.mimosa_dev.MealPlanner.shoppinglist.controller;

import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingList;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListRepository;
import de.mimosa_dev.MealPlanner.shoppinglist.dto.ShoppingListResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * Read-only structured view of the most recently generated shopping list (PRD 9.1 step 11).
 * Generation and item resolution stay chat-driven through the SHOPPING_LIST agent scenario's
 * tools — this mirrors PantryController's "dashboard view of the same data" precedent.
 */
@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {

    private final ShoppingListRepository shoppingListRepository;

    public ShoppingListController(ShoppingListRepository shoppingListRepository) {
        this.shoppingListRepository = shoppingListRepository;
    }

    @GetMapping
    public ShoppingListResponse current(@AuthenticationPrincipal Long userId) {
        ShoppingList mostRecent = shoppingListRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new NoSuchElementException("No shopping list generated yet"));
        ShoppingList withItems = shoppingListRepository.findWithItemsById(mostRecent.getId())
                .orElseThrow(() -> new NoSuchElementException("Shopping list " + mostRecent.getId() + " not found"));
        return ShoppingListResponse.from(withItems);
    }
}
