package de.mimosa_dev.MealPlanner.cooking;

import de.mimosa_dev.MealPlanner.common.Unit;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Recoverable domain error (AI-21a): after scaling by the actual/recipe portions coefficient
 * (FR-53a), one or more ingredients don't have enough ACTIVE pantry stock. Unlike
 * {@link de.mimosa_dev.MealPlanner.pantry.InsufficientStockException} (single product), this
 * carries every shortfall found (FR-53b: "the operation is rejected with the missing positions
 * listed" — plural).
 */
public class CookingInfeasibleException extends RuntimeException {

    public record MissingIngredient(Long productId, Unit unit, BigDecimal needed, BigDecimal available) {
    }

    private final List<MissingIngredient> missingIngredients;

    public CookingInfeasibleException(List<MissingIngredient> missingIngredients) {
        super("Not enough stock to cook this recipe: " + missingIngredients.stream()
                .map(m -> "product %d: need %s %s, have %s %s"
                        .formatted(m.productId(), m.needed(), m.unit(), m.available(), m.unit()))
                .collect(Collectors.joining("; ")));
        this.missingIngredients = missingIngredients;
    }

    public List<MissingIngredient> missingIngredients() {
        return missingIngredients;
    }
}
