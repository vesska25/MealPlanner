package de.mimosa_dev.MealPlanner.profile.onboarding.controller;

import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import de.mimosa_dev.MealPlanner.profile.onboarding.dto.OnboardingStateResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only support for the onboarding chat UI (PRD 9.1 onboarding step) — separate from {@link
 * de.mimosa_dev.MealPlanner.agent.controller.AgentChatController} since it reads profile/draft
 * state, not {@code AgentRunner}.
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingDraftService onboardingDraftService;
    private final UserProfileRepository userProfileRepository;

    public OnboardingController(OnboardingDraftService onboardingDraftService, UserProfileRepository userProfileRepository) {
        this.onboardingDraftService = onboardingDraftService;
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping("/state")
    public OnboardingStateResponse state(@AuthenticationPrincipal Long userId) {
        boolean profileFinalized = userProfileRepository.findByUserId(userId).isPresent();
        return new OnboardingStateResponse(profileFinalized, onboardingDraftService.recentTurns(userId));
    }
}
