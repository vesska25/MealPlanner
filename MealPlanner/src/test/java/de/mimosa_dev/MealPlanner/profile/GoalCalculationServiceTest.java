package de.mimosa_dev.MealPlanner.profile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GoalCalculationServiceTest {

    private final GoalCalculationService service = new GoalCalculationService(new BigDecimal("1200"));

    @Test
    void calculatesTargetsForAMaintainGoal() {
        UserProfile profile = tdeeProfile(Sex.MALE, 30, "180", "80", ActivityLevel.SEDENTARY, Goal.MAINTAIN);

        Optional<GoalTargets> result = service.calculate(profile);

        // BMR = 10*80 + 6.25*180 - 5*30 + 5 = 1780; TDEE = 1780*1.2 = 2136; MAINTAIN leaves it unchanged.
        assertThat(result).isPresent();
        GoalTargets targets = result.get();
        assertThat(targets.dailyKcal()).isEqualByComparingTo("2136.00");
        assertThat(targets.proteinGrams()).isEqualByComparingTo("128.00"); // 1.6 g/kg * 80kg
        assertThat(targets.fatGrams()).isEqualByComparingTo("71.20"); // 30% of kcal / 9
        assertThat(targets.carbsGrams()).isEqualByComparingTo("245.80"); // remainder / 4
        assertThat(targets.flooredToMinimum()).isFalse();
    }

    @Test
    void flooredTargetWhenTheAdjustedCalorieIsBelowTheConfiguredMinimum() {
        // BMR = 10*45 + 6.25*150 - 5*70 - 161 = 876.5; TDEE = 876.5*1.2 = 1051.8;
        // LOSE_WEIGHT * 0.8 = 841.44, below the 1200 floor -> INV-11 clamps it.
        UserProfile profile = tdeeProfile(Sex.FEMALE, 70, "150", "45", ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);

        GoalTargets targets = service.calculate(profile).orElseThrow();

        assertThat(targets.dailyKcal()).isEqualByComparingTo("1200.00");
        assertThat(targets.proteinGrams()).isEqualByComparingTo("72.00"); // 1.6 g/kg * 45kg
        assertThat(targets.fatGrams()).isEqualByComparingTo("40.00"); // 30% of 1200 / 9
        assertThat(targets.carbsGrams()).isEqualByComparingTo("138.00"); // remainder / 4
        assertThat(targets.flooredToMinimum()).isTrue();
    }

    @Test
    void returnsEmptyWhenAnyTdeeInputIsMissing() {
        UserProfile profile = new UserProfile(1L, 2, 60);
        profile.setSex(Sex.MALE);
        profile.setAgeYears(30);
        profile.setHeightCm(new BigDecimal("180"));
        // weightKg deliberately left unset (null) — FR-13: declining any TDEE input just leaves
        // block "Б" disabled, it never blocks onboarding itself.
        profile.setActivityLevel(ActivityLevel.SEDENTARY);

        assertThat(service.calculate(profile)).isEmpty();
    }

    @Test
    void gainWeightGoalUsesAHigherProteinTargetThanMaintain() {
        UserProfile maintain = tdeeProfile(Sex.MALE, 30, "180", "80", ActivityLevel.SEDENTARY, Goal.MAINTAIN);
        UserProfile gain = tdeeProfile(Sex.MALE, 30, "180", "80", ActivityLevel.SEDENTARY, Goal.GAIN_WEIGHT);

        BigDecimal maintainProtein = service.calculate(maintain).orElseThrow().proteinGrams();
        BigDecimal gainProtein = service.calculate(gain).orElseThrow().proteinGrams();

        assertThat(maintainProtein).isEqualByComparingTo("128.00"); // 1.6 g/kg
        assertThat(gainProtein).isEqualByComparingTo("144.00"); // 1.8 g/kg
    }

    @Test
    void proteinAndFatAreCappedSoCarbsNeverGoNegative() {
        // An extreme, unrealistic bodyweight deliberately chosen so protein demand (1.8 g/kg for
        // GAIN_WEIGHT) alone would exceed 40% of the target — isolating the protein+fat-vs-70%-cap
        // branch, not a real user scenario.
        UserProfile profile = tdeeProfile(Sex.MALE, 25, "170", "500", ActivityLevel.SEDENTARY, Goal.GAIN_WEIGHT);

        GoalTargets targets = service.calculate(profile).orElseThrow();

        // BMR = 10*500 + 6.25*170 - 5*25 + 5 = 5942.5; TDEE = 5942.5*1.2 = 7131; *1.15 = 8200.65.
        assertThat(targets.dailyKcal()).isEqualByComparingTo("8200.65");
        assertThat(targets.carbsGrams()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        // Uncapped protein would be 1.8*500 = 900g — confirms the cap actually scaled it down.
        assertThat(targets.proteinGrams()).isLessThan(new BigDecimal("900.00"));
    }

    private static UserProfile tdeeProfile(
            Sex sex, int age, String heightCm, String weightKg, ActivityLevel activityLevel, Goal goal) {
        UserProfile profile = new UserProfile(1L, 2, 60);
        profile.setSex(sex);
        profile.setAgeYears(age);
        profile.setHeightCm(new BigDecimal(heightCm));
        profile.setWeightKg(new BigDecimal(weightKg));
        profile.setActivityLevel(activityLevel);
        profile.setGoal(goal);
        return profile;
    }
}
