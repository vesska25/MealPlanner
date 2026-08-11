package de.mimosa_dev.MealPlanner.pantry.controller;

import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.pantry.dto.PantryItemResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only (PRD 9.1 step 11) — pantry mutation stays chat-driven through the existing
 * pantry-assistant tools; this is just a dashboard view of the same data.
 */
@RestController
@RequestMapping("/api/pantry")
public class PantryController {

    private final PantryItemRepository pantryItemRepository;

    public PantryController(PantryItemRepository pantryItemRepository) {
        this.pantryItemRepository = pantryItemRepository;
    }

    @GetMapping
    public List<PantryItemResponse> activeItems(@AuthenticationPrincipal Long userId) {
        return pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(userId, PantryItemStatus.ACTIVE).stream()
                .map(PantryItemResponse::from)
                .toList();
    }
}
