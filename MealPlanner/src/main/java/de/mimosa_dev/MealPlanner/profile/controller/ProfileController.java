package de.mimosa_dev.MealPlanner.profile.controller;

import de.mimosa_dev.MealPlanner.profile.GoalCalculationService;
import de.mimosa_dev.MealPlanner.profile.UserProfile;
import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import de.mimosa_dev.MealPlanner.profile.dto.GoalsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-70/FR-74: thin wrapper over {@link GoalCalculationService} (no LLM, no new domain logic
 * here — the calculation itself lives entirely in step 13 Phase A). Returns 404, not zeroed
 * numbers, whenever goals are off or incomplete — FR-74 requires the numbers to appear nowhere
 * in the interface when disabled, so there's no payload to leak even via devtools.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserProfileRepository userProfileRepository;
    private final GoalCalculationService goalCalculationService;

    public ProfileController(UserProfileRepository userProfileRepository, GoalCalculationService goalCalculationService) {
        this.userProfileRepository = userProfileRepository;
        this.goalCalculationService = goalCalculationService;
    }

    @GetMapping("/goals")
    public ResponseEntity<GoalsResponse> goals(@AuthenticationPrincipal Long userId) {
        return userProfileRepository.findByUserId(userId)
                .filter(UserProfile::isGoalsEnabled)
                .flatMap(goalCalculationService::calculate)
                .map(targets -> ResponseEntity.ok(GoalsResponse.from(targets)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
