package de.mimosa_dev.MealPlanner.pantry;

import de.mimosa_dev.MealPlanner.common.Unit;

import java.math.BigDecimal;

/**
 * Recoverable domain error (AI-21a): not enough ACTIVE stock to satisfy a
 * {@link PantryService#consume} call. Belongs in the agent's reasoning loop as an
 * observation once the agent layer exists (step 6) — never an infrastructure-level failure.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, Unit unit, BigDecimal requested, BigDecimal available) {
        super("Insufficient stock for product %d: requested %s %s, available %s %s"
                .formatted(productId, requested, unit, available, unit));
    }
}
