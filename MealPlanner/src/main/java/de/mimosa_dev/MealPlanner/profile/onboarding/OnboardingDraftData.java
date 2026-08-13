package de.mimosa_dev.MealPlanner.profile.onboarding;

import java.math.BigDecimal;
import java.util.Set;

/**
 * The structured shape of {@link OnboardingDraft#getDraftJson()} once deserialized. Every field
 * is nullable except {@code allergiesCollected} — FR-11's mandatory fields are collected one
 * tool call at a time, so a partially-filled draft is the normal in-progress state, not an error.
 *
 * @param allergiesCollected distinguishes "asked, and the answer was zero exclusions" from "not
 *                            asked yet" — an empty {@code excludedProductIds} alone can't tell
 *                            those apart, but {@link OnboardingDraftService#finalizeProfile}
 *                            needs to (FR-11 requires allergies to actually be asked)
 */
public record OnboardingDraftData(
        Integer householdSize,
        Integer maxCookTimeWeekdayMinutes,
        Set<Long> excludedProductIds,
        boolean allergiesCollected,
        Set<String> equipment,
        Set<String> freeDays,
        String goal,
        BigDecimal weeklyBudget,
        String preferredStores,
        String country,
        String sex,
        Integer ageYears,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String activityLevel) {

    static OnboardingDraftData empty() {
        return new OnboardingDraftData(
                null, null, null, false, null, null, null, null, null, null, null, null, null, null, null);
    }
}
