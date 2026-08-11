package de.mimosa_dev.MealPlanner.cooking;

import java.math.BigDecimal;

/**
 * INV-09: eaten + remaining portions can never exceed what was originally cooked. Service-level
 * guard alongside the {@code chk_cooked_dish_portions_remaining_range} DB constraint (the domain
 * invariant needs both, per CLAUDE.md's rule for the invariants list).
 */
public class InsufficientPortionsException extends RuntimeException {

    public InsufficientPortionsException(Long cookedDishId, BigDecimal requested, BigDecimal available) {
        super("Cooked dish %d has only %s portions remaining, requested %s"
                .formatted(cookedDishId, available, requested));
    }
}
