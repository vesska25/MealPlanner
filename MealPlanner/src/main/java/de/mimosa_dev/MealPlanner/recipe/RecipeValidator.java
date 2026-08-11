package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hard-constraint validator for a generated recipe (PRD 9.1 step 5, AI-08). A recipe that
 * fails this never reaches the user — enforcement is architectural (AI-09, INV-04), not a
 * prompt instruction the model could ignore or be talked around. Built before the agent layer
 * (step 6) on purpose, so the agent sits on top of an already-validated domain.
 *
 * <p>Checks: allergies/hard exclusions (INV-04), pantry feasibility for non-staple ingredients
 * (FR-29 — staples are assumed always available and skipped), required equipment, and cook
 * time. Unit validity (AI-04) is enforced by {@link RecipeIngredient#unit()}'s type, not
 * re-checked here — there's nothing to validate once a value has successfully become a
 * {@code Unit} enum constant.
 */
@Service
public class RecipeValidator {

    private final ProductRepository productRepository;
    private final PantryItemRepository pantryItemRepository;

    public RecipeValidator(ProductRepository productRepository, PantryItemRepository pantryItemRepository) {
        this.productRepository = productRepository;
        this.pantryItemRepository = pantryItemRepository;
    }

    public RecipeValidationResult validate(Long userId, RecipeCandidate recipe, UserConstraints constraints) {
        List<RecipeViolation> violations = new ArrayList<>();

        for (RecipeIngredient ingredient : recipe.ingredients()) {
            Optional<Product> product = productRepository.findById(ingredient.productId());
            if (product.isEmpty()) {
                violations.add(new RecipeViolation(RecipeViolationType.UNKNOWN_PRODUCT,
                        "Recipe references unknown product id " + ingredient.productId()));
                continue; // can't check exclusion/stock for a product that doesn't exist
            }
            checkNotExcluded(product.get(), constraints, violations);
            checkPantryFeasibility(userId, product.get(), ingredient, violations);
        }

        checkEquipment(recipe, constraints, violations);
        checkCookTime(recipe, constraints, violations);

        return violations.isEmpty() ? RecipeValidationResult.success() : RecipeValidationResult.invalid(violations);
    }

    private static void checkNotExcluded(Product product, UserConstraints constraints, List<RecipeViolation> violations) {
        if (constraints.excludedProductIds().contains(product.getId())) {
            violations.add(new RecipeViolation(RecipeViolationType.ALLERGEN_OR_EXCLUDED_INGREDIENT,
                    "Recipe contains an excluded/allergenic ingredient: " + product.getCanonicalName()));
        }
    }

    private void checkPantryFeasibility(Long userId, Product product, RecipeIngredient ingredient, List<RecipeViolation> violations) {
        if (product.isStaple()) {
            return; // FR-29: staples are excluded from stock accounting entirely
        }
        BigDecimal available = pantryItemRepository.sumQuantityByUserAndProductAndUnitAndStatus(
                userId, product.getId(), ingredient.unit(), PantryItemStatus.ACTIVE);
        if (available.compareTo(ingredient.quantity()) < 0) {
            violations.add(new RecipeViolation(RecipeViolationType.INSUFFICIENT_PANTRY_STOCK,
                    "Not enough %s: need %s %s, have %s %s".formatted(
                            product.getCanonicalName(), ingredient.quantity(), ingredient.unit(),
                            available, ingredient.unit())));
        }
    }

    private static void checkEquipment(RecipeCandidate recipe, UserConstraints constraints, List<RecipeViolation> violations) {
        Set<String> available = normalize(constraints.availableEquipment());
        for (String required : recipe.requiredEquipment()) {
            if (!available.contains(normalize(required))) {
                violations.add(new RecipeViolation(RecipeViolationType.MISSING_EQUIPMENT,
                        "Missing equipment: " + required));
            }
        }
    }

    private static void checkCookTime(RecipeCandidate recipe, UserConstraints constraints, List<RecipeViolation> violations) {
        if (recipe.cookTimeMinutes() > constraints.maxCookTimeMinutes()) {
            violations.add(new RecipeViolation(RecipeViolationType.COOK_TIME_EXCEEDS_LIMIT,
                    "Recipe takes %d min, limit is %d min".formatted(
                            recipe.cookTimeMinutes(), constraints.maxCookTimeMinutes())));
        }
    }

    private static Set<String> normalize(Set<String> values) {
        return values.stream().map(RecipeValidator::normalize).collect(Collectors.toSet());
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
