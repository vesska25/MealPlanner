package de.mimosa_dev.MealPlanner.profile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Deterministic target-calorie and macro calculation (FR-70, AI-02) — the model never
 * participates in this arithmetic (AI-07). PRD 6.1's INV-11 ("target calories never below
 * MIN_ALLOWED_DAILY_CALORIES, a config constant, not an individually computed medical value")
 * is enforced in exactly one place: {@link #calculate}'s floor clamp below.
 *
 * <p>The PRD names the Mifflin-St Jeor formula for the calorie target (FR-70) but gives no
 * concrete goal-adjustment percentages or macro-split formula — both are documented judgment
 * calls here, flagged for Sergio's eventual sign-off, same treatment step 4's
 * MILLILITER-as-gram-equivalent simplification got:
 * <ul>
 *     <li>Goal adjustment is percentage-based (not a flat kcal offset) so it scales with body
 *     size: {@code LOSE_WEIGHT} -20%, {@code GAIN_WEIGHT} +15%, {@code MAINTAIN}/{@code VARIETY}
 *     unchanged.</li>
 *     <li>Macro split: protein 1.6 g/kg bodyweight (1.8 g/kg for {@code GAIN_WEIGHT}), fat 30%
 *     of target kcal, carbs the remainder — with protein+fat capped at 70% of target kcal first
 *     so a floored, low-calorie target can never push carbs negative.</li>
 * </ul>
 */
@Service
public class GoalCalculationService {

    private static final int RESULT_SCALE = 2;
    private static final BigDecimal MALE_BMR_OFFSET = BigDecimal.valueOf(5);
    private static final BigDecimal FEMALE_BMR_OFFSET = BigDecimal.valueOf(-161);

    private static final BigDecimal LOSE_WEIGHT_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal GAIN_WEIGHT_FACTOR = new BigDecimal("1.15");

    private static final BigDecimal PROTEIN_G_PER_KG = new BigDecimal("1.6");
    private static final BigDecimal PROTEIN_G_PER_KG_GAIN = new BigDecimal("1.8");
    private static final BigDecimal FAT_SHARE_OF_KCAL = new BigDecimal("0.30");
    private static final BigDecimal PROTEIN_FAT_KCAL_CAP_SHARE = new BigDecimal("0.70");
    private static final BigDecimal KCAL_PER_GRAM_PROTEIN = BigDecimal.valueOf(4);
    private static final BigDecimal KCAL_PER_GRAM_FAT = BigDecimal.valueOf(9);
    private static final BigDecimal KCAL_PER_GRAM_CARBS = BigDecimal.valueOf(4);

    private final BigDecimal minAllowedDailyCalories;

    public GoalCalculationService(
            @Value("${goals.min-allowed-daily-calories:1200}") BigDecimal minAllowedDailyCalories) {
        this.minAllowedDailyCalories = minAllowedDailyCalories;
    }

    /**
     * @return empty if any FR-13 TDEE input is missing on the profile — block "Б" (goals) just
     *         stays off in that case, per FR-13, it never blocks onboarding itself
     */
    public Optional<GoalTargets> calculate(UserProfile profile) {
        if (profile.getSex() == null || profile.getAgeYears() == null || profile.getHeightCm() == null
                || profile.getWeightKg() == null || profile.getActivityLevel() == null) {
            return Optional.empty();
        }

        BigDecimal bmr = bmr(profile);
        BigDecimal tdee = bmr.multiply(profile.getActivityLevel().multiplier());
        BigDecimal adjustedTarget = tdee.multiply(goalFactor(profile.getGoal()));

        boolean flooredToMinimum = adjustedTarget.compareTo(minAllowedDailyCalories) < 0;
        BigDecimal target = flooredToMinimum ? minAllowedDailyCalories : adjustedTarget;

        return Optional.of(macros(target, profile, flooredToMinimum));
    }

    private static BigDecimal bmr(UserProfile profile) {
        BigDecimal sexOffset = profile.getSex() == Sex.MALE ? MALE_BMR_OFFSET : FEMALE_BMR_OFFSET;
        return BigDecimal.TEN.multiply(profile.getWeightKg())
                .add(new BigDecimal("6.25").multiply(profile.getHeightCm()))
                .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(profile.getAgeYears())))
                .add(sexOffset);
    }

    private static BigDecimal goalFactor(Goal goal) {
        if (goal == null) {
            return BigDecimal.ONE;
        }
        return switch (goal) {
            case LOSE_WEIGHT -> LOSE_WEIGHT_FACTOR;
            case GAIN_WEIGHT -> GAIN_WEIGHT_FACTOR;
            // VARIETY isn't a weight goal (FR-12/FR-13 only ask for TDEE data on weight goals),
            // so this branch is a defensive default rather than an expected real case.
            case MAINTAIN, VARIETY -> BigDecimal.ONE;
        };
    }

    private static GoalTargets macros(BigDecimal targetKcal, UserProfile profile, boolean flooredToMinimum) {
        BigDecimal proteinGPerKg = profile.getGoal() == Goal.GAIN_WEIGHT ? PROTEIN_G_PER_KG_GAIN : PROTEIN_G_PER_KG;
        BigDecimal proteinGrams = proteinGPerKg.multiply(profile.getWeightKg());
        BigDecimal proteinKcal = proteinGrams.multiply(KCAL_PER_GRAM_PROTEIN);
        BigDecimal fatKcal = targetKcal.multiply(FAT_SHARE_OF_KCAL);

        BigDecimal proteinFatCap = targetKcal.multiply(PROTEIN_FAT_KCAL_CAP_SHARE);
        BigDecimal proteinFatKcal = proteinKcal.add(fatKcal);
        if (proteinFatKcal.compareTo(proteinFatCap) > 0) {
            BigDecimal scaleDown = proteinFatCap.divide(proteinFatKcal, 10, RoundingMode.HALF_UP);
            proteinKcal = proteinKcal.multiply(scaleDown);
            fatKcal = fatKcal.multiply(scaleDown);
            proteinGrams = proteinKcal.divide(KCAL_PER_GRAM_PROTEIN, 10, RoundingMode.HALF_UP);
        }
        BigDecimal fatGrams = fatKcal.divide(KCAL_PER_GRAM_FAT, 10, RoundingMode.HALF_UP);

        BigDecimal carbsKcal = targetKcal.subtract(proteinKcal).subtract(fatKcal).max(BigDecimal.ZERO);
        BigDecimal carbsGrams = carbsKcal.divide(KCAL_PER_GRAM_CARBS, 10, RoundingMode.HALF_UP);

        return new GoalTargets(
                scale(targetKcal), scale(proteinGrams), scale(fatGrams), scale(carbsGrams), flooredToMinimum);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}
