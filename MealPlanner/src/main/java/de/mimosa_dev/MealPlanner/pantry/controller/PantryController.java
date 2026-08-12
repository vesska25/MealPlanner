package de.mimosa_dev.MealPlanner.pantry.controller;

import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.pantry.dto.DiscardPantryItemRequest;
import de.mimosa_dev.MealPlanner.pantry.dto.PantryItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mostly read-only (PRD 9.1 step 11) — the dashboard view of pantry stock, plus one deliberate
 * exception: discard. Every other mutation (add stock, consume) stays chat-driven through the
 * pantry-assistant tools; discard gets a real REST action here (mirroring
 * {@code CookingController}'s precedent for cooking confirmation) so the web client can offer a
 * "Mark spoiled" button without round-tripping through the chat scenario for a single-field action.
 */
@RestController
@RequestMapping("/api/pantry")
public class PantryController {

    private final PantryItemRepository pantryItemRepository;
    private final PantryService pantryService;

    public PantryController(PantryItemRepository pantryItemRepository, PantryService pantryService) {
        this.pantryItemRepository = pantryItemRepository;
        this.pantryService = pantryService;
    }

    @GetMapping
    public List<PantryItemResponse> activeItems(@AuthenticationPrincipal Long userId) {
        return pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(userId, PantryItemStatus.ACTIVE).stream()
                .map(PantryItemResponse::from)
                .toList();
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<Void> discard(
            @AuthenticationPrincipal Long userId, @PathVariable Long id,
            @Valid @RequestBody DiscardPantryItemRequest request) {
        pantryService.discard(userId, id, request.reason());
        return ResponseEntity.noContent().build();
    }
}
