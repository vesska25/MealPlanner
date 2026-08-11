package de.mimosa_dev.MealPlanner.product;

import de.mimosa_dev.MealPlanner.common.Unit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Deterministic KБЖУ (calorie/protein/fat/carb) calculation for a quantity of a product (PRD
 * 9.1 step 4, AI-02). The model never computes this — arithmetic on nutrient values is exactly
 * what AI-02 reserves for Java code.
 */
@Service
public class NutritionCalculationService {

    private static final int RESULT_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * @return empty if the product has no nutrition data at all (FR-43b — unverified or
     *         unmatched products), or if {@code unit} is {@code PIECE} and the product has no
     *         {@code grams_per_piece} conversion factor (AI-05)
     */
    public Optional<NutritionValues> calculate(Product product, BigDecimal quantity, Unit unit) {
        if (product.getKcalPer100g() == null || product.getProteinPer100g() == null
                || product.getFatPer100g() == null || product.getCarbsPer100g() == null) {
            return Optional.empty();
        }

        return toGrams(product, quantity, unit).map(grams -> {
            BigDecimal factor = grams.divide(HUNDRED, 10, RoundingMode.HALF_UP);
            return new NutritionValues(
                    scale(product.getKcalPer100g().multiply(factor)),
                    scale(product.getProteinPer100g().multiply(factor)),
                    scale(product.getFatPer100g().multiply(factor)),
                    scale(product.getCarbsPer100g().multiply(factor)));
        });
    }

    private static Optional<BigDecimal> toGrams(Product product, BigDecimal quantity, Unit unit) {
        return switch (unit) {
            case GRAM -> Optional.of(quantity);
            // The product catalogue has no density field (PRD gap — flagged, not invented
            // around): MILLILITER is treated as gram-equivalent (~1 g/ml). Acceptable for the
            // optional nutrition block ("Б" layer in the PRD); revisit if precision matters.
            case MILLILITER -> Optional.of(quantity);
            case PIECE -> Optional.ofNullable(product.getGramsPerPiece()).map(quantity::multiply);
        };
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}
