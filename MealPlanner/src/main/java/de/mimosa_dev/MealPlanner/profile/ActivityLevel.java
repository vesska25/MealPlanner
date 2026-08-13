package de.mimosa_dev.MealPlanner.profile;

import java.math.BigDecimal;

/**
 * FR-13: a fixed set of described options, not a free-form number. Multipliers are the
 * standard TDEE-from-BMR factors; the PRD names the formula but not these specific values.
 */
public enum ActivityLevel {
    SEDENTARY(new BigDecimal("1.2")),
    LIGHT(new BigDecimal("1.375")),
    MODERATE(new BigDecimal("1.55")),
    ACTIVE(new BigDecimal("1.725")),
    VERY_ACTIVE(new BigDecimal("1.9"));

    private final BigDecimal multiplier;

    ActivityLevel(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public BigDecimal multiplier() {
        return multiplier;
    }
}
