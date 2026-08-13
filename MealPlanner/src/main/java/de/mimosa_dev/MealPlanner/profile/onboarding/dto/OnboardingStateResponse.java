package de.mimosa_dev.MealPlanner.profile.onboarding.dto;

import de.mimosa_dev.MealPlanner.profile.onboarding.DialogueTurn;

import java.util.List;

/**
 * FR-10b: lets the frontend resume a lost/closed onboarding session by hydrating its chat panel
 * from {@code recentTurns} instead of starting over. {@code profileFinalized} drives the
 * scenario-picker screen once onboarding is done.
 */
public record OnboardingStateResponse(boolean profileFinalized, List<DialogueTurn> recentTurns) {
}
