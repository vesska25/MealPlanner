package de.mimosa_dev.MealPlanner.mealentry.controller;

import de.mimosa_dev.MealPlanner.mealentry.MealEntryService;
import de.mimosa_dev.MealPlanner.mealentry.dto.CreateMealEntryRequest;
import de.mimosa_dev.MealPlanner.mealentry.dto.MealEntryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-82's "day status" — used by both the Telegram bot's inline buttons and, per FR-83, any future web UI. */
@RestController
@RequestMapping("/api/meal-entries")
public class MealEntryController {

    private final MealEntryService mealEntryService;

    public MealEntryController(MealEntryService mealEntryService) {
        this.mealEntryService = mealEntryService;
    }

    @PostMapping
    public MealEntryResponse create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateMealEntryRequest request) {
        return MealEntryResponse.from(mealEntryService.recordOther(
                userId, request.type(), request.kcal(), request.proteinGrams(), request.fatGrams(), request.carbsGrams()));
    }
}
