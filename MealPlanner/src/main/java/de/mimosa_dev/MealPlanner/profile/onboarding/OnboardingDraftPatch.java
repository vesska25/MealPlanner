package de.mimosa_dev.MealPlanner.profile.onboarding;

import java.math.BigDecimal;
import java.util.Set;

/**
 * One {@code update_onboarding_draft} tool call's worth of updates. A {@code null} field means
 * "not mentioned this call, leave the draft's existing value alone" — never "clear it"; nothing
 * in FR-10/FR-11 needs onboarding to un-collect an already-answered field. {@code
 * excludedProductIds} is the one field where {@code null} vs. non-null (even an empty set)
 * matters beyond that: see {@link OnboardingDraftData#allergiesCollected()}.
 */
public record OnboardingDraftPatch(
        Integer householdSize,
        Integer maxCookTimeWeekdayMinutes,
        Set<Long> excludedProductIds,
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
}
