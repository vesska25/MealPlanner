package de.mimosa_dev.MealPlanner.profile.onboarding;

/** One message in {@link OnboardingDraft#getRecentTurnsJson()}. {@code role} is "user" or "agent". */
public record DialogueTurn(String role, String text) {
}
