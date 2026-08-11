package de.mimosa_dev.MealPlanner.product;

import de.mimosa_dev.MealPlanner.common.Unit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NutritionCalculationServiceTest {

    private final NutritionCalculationService service = new NutritionCalculationService();

    @Test
    void calculatesFromGrams() {
        Product egg = productWithNutrition(new BigDecimal("143"), new BigDecimal("12.6"),
                new BigDecimal("9.5"), new BigDecimal("0.7"));

        Optional<NutritionValues> result = service.calculate(egg, new BigDecimal("200"), Unit.GRAM);

        assertThat(result).contains(new NutritionValues(
                new BigDecimal("286.00"), new BigDecimal("25.20"),
                new BigDecimal("19.00"), new BigDecimal("1.40")));
    }

    @Test
    void convertsPieceToGramsBeforeCalculating() {
        Product egg = productWithNutrition(new BigDecimal("143"), new BigDecimal("12.6"),
                new BigDecimal("9.5"), new BigDecimal("0.7"));
        egg.setGramsPerPiece(new BigDecimal("50"));

        Optional<NutritionValues> result = service.calculate(egg, new BigDecimal("2"), Unit.PIECE);

        // 2 pieces * 50g = 100g, i.e. exactly the per-100g values
        assertThat(result).contains(new NutritionValues(
                new BigDecimal("143.00"), new BigDecimal("12.60"),
                new BigDecimal("9.50"), new BigDecimal("0.70")));
    }

    @Test
    void pieceWithoutGramsPerPieceIsUnavailable() {
        Product egg = productWithNutrition(new BigDecimal("143"), new BigDecimal("12.6"),
                new BigDecimal("9.5"), new BigDecimal("0.7"));
        // gramsPerPiece left null

        Optional<NutritionValues> result = service.calculate(egg, new BigDecimal("2"), Unit.PIECE);

        assertThat(result).isEmpty();
    }

    @Test
    void milliliterIsTreatedAsGramEquivalent() {
        Product milk = productWithNutrition(new BigDecimal("42"), new BigDecimal("3.4"),
                new BigDecimal("1"), new BigDecimal("5"));

        Optional<NutritionValues> result = service.calculate(milk, new BigDecimal("250"), Unit.MILLILITER);

        assertThat(result).contains(new NutritionValues(
                new BigDecimal("105.00"), new BigDecimal("8.50"),
                new BigDecimal("2.50"), new BigDecimal("12.50")));
    }

    @Test
    void missingNutritionDataIsUnavailable() {
        Product unverified = new Product();
        unverified.setCanonicalName("mystery item");
        unverified.setCategory(ProductCategory.OTHER);
        unverified.setDefaultShelfLifeDays(7);
        // no kcal/protein/fat/carbs set — mirrors an unverified, unmatched product (FR-43b)

        Optional<NutritionValues> result = service.calculate(unverified, new BigDecimal("100"), Unit.GRAM);

        assertThat(result).isEmpty();
    }

    private static Product productWithNutrition(BigDecimal kcal, BigDecimal protein, BigDecimal fat, BigDecimal carbs) {
        Product product = new Product();
        product.setCanonicalName("test product");
        product.setCategory(ProductCategory.OTHER);
        product.setDefaultShelfLifeDays(7);
        product.setKcalPer100g(kcal);
        product.setProteinPer100g(protein);
        product.setFatPer100g(fat);
        product.setCarbsPer100g(carbs);
        return product;
    }
}
