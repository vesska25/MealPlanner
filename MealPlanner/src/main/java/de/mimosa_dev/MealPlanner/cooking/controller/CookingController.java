package de.mimosa_dev.MealPlanner.cooking.controller;

import de.mimosa_dev.MealPlanner.cooking.CookedDishRepository;
import de.mimosa_dev.MealPlanner.cooking.CookedDishService;
import de.mimosa_dev.MealPlanner.cooking.CookedDishStatus;
import de.mimosa_dev.MealPlanner.cooking.CookingService;
import de.mimosa_dev.MealPlanner.cooking.dto.CookedDishResponse;
import de.mimosa_dev.MealPlanner.cooking.dto.ConfirmCookingRequest;
import de.mimosa_dev.MealPlanner.cooking.dto.ConsumePortionsRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The one required piece of PRD step 11's REST surface — {@link CookingService} and
 * {@link CookedDishService} are deliberately not agent-tool-exposed (AI-13), so without this
 * controller the cooking transaction built in step 8 has no way to ever be called at all.
 */
@RestController
public class CookingController {

    private final CookingService cookingService;
    private final CookedDishService cookedDishService;
    private final CookedDishRepository cookedDishRepository;

    public CookingController(
            CookingService cookingService, CookedDishService cookedDishService, CookedDishRepository cookedDishRepository) {
        this.cookingService = cookingService;
        this.cookedDishService = cookedDishService;
        this.cookedDishRepository = cookedDishRepository;
    }

    @PostMapping("/api/cooking/confirm")
    public CookedDishResponse confirm(@AuthenticationPrincipal Long userId, @Valid @RequestBody ConfirmCookingRequest request) {
        var dish = cookingService.confirmCooking(userId, request.recipeId(), request.actualPortions(), request.idempotencyKey());
        return CookedDishResponse.from(dish);
    }

    @GetMapping("/api/cooked-dishes")
    public List<CookedDishResponse> activeCookedDishes(@AuthenticationPrincipal Long userId) {
        return cookedDishRepository.findByUserIdAndStatusWithRecipe(userId, CookedDishStatus.ACTIVE).stream()
                .map(CookedDishResponse::from)
                .toList();
    }

    @PostMapping("/api/cooked-dishes/{id}/consume")
    public ResponseEntity<Void> consume(
            @AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody ConsumePortionsRequest request) {
        cookedDishService.consumePortions(userId, id, request.portionsEaten());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/cooked-dishes/{id}")
    public ResponseEntity<Void> discard(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        cookedDishService.discard(userId, id);
        return ResponseEntity.noContent().build();
    }
}
