package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic candidate ranking (PRD 9.1 step 7, FR-31). The model generates candidates
 * (FR-34); this is the code that ranks them — the model's own judgment of which is "best"
 * carries no weight (AI-07).
 *
 * <p>FR-31 names six factors. Three of them — leftover usage (needs the cooked-dish inventory
 * from step 8), preference alignment (needs preference_signal from step 10), and novelty
 * (needs meal_entry history from step 8) — have no real data source yet. They're kept in the
 * formula at their documented weight so the seam for wiring them in is explicit in the code,
 * but every candidate gets the same neutral contribution for them today, so they don't affect
 * relative ranking until real data exists to distinguish candidates by them.
 */
@Service
public class RecipeCandidateScorer {

    // Weights sum to 1.0.
    private static final double EXPIRY_URGENCY_WEIGHT = 0.35;
    private static final double PANTRY_COVERAGE_WEIGHT = 0.30;
    private static final double COOK_TIME_WEIGHT = 0.15;
    private static final double LEFTOVER_USAGE_WEIGHT = 0.10; // always 0.0 until step 8
    private static final double PREFERENCE_WEIGHT = 0.05;     // always 0.0 until step 10
    private static final double NOVELTY_WEIGHT = 0.05;        // always 0.5 (neutral) until step 8

    private static final int URGENCY_HORIZON_DAYS = 14;
    private static final int COOK_TIME_CEILING_MINUTES = 60;

    private final ProductRepository productRepository;
    private final PantryItemRepository pantryItemRepository;

    public RecipeCandidateScorer(ProductRepository productRepository, PantryItemRepository pantryItemRepository) {
        this.productRepository = productRepository;
        this.pantryItemRepository = pantryItemRepository;
    }

    public double score(Long userId, RecipeCandidate recipe) {
        return EXPIRY_URGENCY_WEIGHT * expiryUrgency(userId, recipe)
                + PANTRY_COVERAGE_WEIGHT * pantryCoverage(userId, recipe)
                + COOK_TIME_WEIGHT * cookTimeScore(recipe)
                + LEFTOVER_USAGE_WEIGHT * 0.0
                + PREFERENCE_WEIGHT * 0.0
                + NOVELTY_WEIGHT * 0.5;
    }

    /**
     * The highest urgency among the recipe's trackable ingredients — an ingredient about to
     * expire makes the whole recipe urgent, even if its other ingredients aren't at risk.
     * Urgency for one ingredient is a deterministic function of its <em>earliest</em> matching
     * pantry position's expiry date (FR-31a: ties in expiry date never depend on row order,
     * because {@code min()} over dates is well-defined regardless of order).
     */
    private double expiryUrgency(Long userId, RecipeCandidate recipe) {
        double maxUrgency = 0.0;
        for (RecipeIngredient ingredient : recipe.ingredients()) {
            if (isStaple(ingredient)) {
                continue;
            }
            Optional<LocalDate> earliestExpiry = pantryItemRepository
                    .findByUserIdAndProductIdAndUnitAndStatusOrderByExpiresAtAsc(
                            userId, ingredient.productId(), ingredient.unit(), PantryItemStatus.ACTIVE)
                    .stream()
                    .map(PantryItem::getExpiresAt)
                    .min(Comparator.naturalOrder());
            if (earliestExpiry.isEmpty()) {
                continue; // not in pantry at all — nothing at risk of spoiling
            }
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), earliestExpiry.get());
            double urgency = clamp(1.0 - (double) daysUntilExpiry / URGENCY_HORIZON_DAYS, 0.0, 1.0);
            maxUrgency = Math.max(maxUrgency, urgency);
        }
        return maxUrgency;
    }

    /** Fraction of trackable (non-staple) ingredients already sufficiently stocked (FR-29). */
    private double pantryCoverage(Long userId, RecipeCandidate recipe) {
        List<RecipeIngredient> trackable = recipe.ingredients().stream()
                .filter(ingredient -> !isStaple(ingredient))
                .toList();
        if (trackable.isEmpty()) {
            return 1.0; // nothing that needs to be bought
        }
        long covered = trackable.stream().filter(ingredient -> {
            BigDecimal available = pantryItemRepository.sumQuantityByUserAndProductAndUnitAndStatus(
                    userId, ingredient.productId(), ingredient.unit(), PantryItemStatus.ACTIVE);
            return available.compareTo(ingredient.quantity()) >= 0;
        }).count();
        return (double) covered / trackable.size();
    }

    private double cookTimeScore(RecipeCandidate recipe) {
        return clamp(1.0 - (double) recipe.cookTimeMinutes() / COOK_TIME_CEILING_MINUTES, 0.0, 1.0);
    }

    private boolean isStaple(RecipeIngredient ingredient) {
        return productRepository.findById(ingredient.productId()).map(Product::isStaple).orElse(false);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
