package de.mimosa_dev.MealPlanner.account.dto;

import java.math.BigDecimal;
import java.util.Set;

public record UserProfileExport(
        Integer householdSize, Integer maxCookTimeWeekdayMinutes, Set<String> excludedProductNames,
        Set<String> equipment, Set<String> freeDays, String goal, BigDecimal weeklyBudget,
        String preferredStores, String country, String sex, Integer ageYears, BigDecimal heightCm,
        BigDecimal weightKg, String activityLevel, boolean goalsEnabled) {
}
