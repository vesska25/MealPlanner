package de.mimosa_dev.MealPlanner.cooking;

import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.product.NutritionCalculationService;
import de.mimosa_dev.MealPlanner.product.NutritionValues;
import de.mimosa_dev.MealPlanner.recipe.DishCategory;
import de.mimosa_dev.MealPlanner.recipe.DishCategoryResolver;
import de.mimosa_dev.MealPlanner.recipe.DishCategoryShelfLife;
import de.mimosa_dev.MealPlanner.recipe.DishCategoryShelfLifeRepository;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The cooking transaction (PRD 9.1 step 8): confirm cooking, scale ingredient deduction to the
 * actual number of portions made, and create the resulting cooked-dish inventory record — all
 * in one atomic, idempotent operation (FR-56, FR-57). Not agent-tool-exposed: AI-13 excludes
 * cooked-dish creation from the meal-selection scenario's tool set, so this is called directly
 * by whatever future entry point (REST, Telegram) confirms cooking on the user's behalf.
 */
@Service
public class CookingService {

    private static final int SCALE_FACTOR_PRECISION = 10;
    private static final int QUANTITY_SCALE = 3;
    private static final int NUTRITION_SCALE = 2;

    private final RecipeRepository recipeRepository;
    private final PantryItemRepository pantryItemRepository;
    private final CookedDishRepository cookedDishRepository;
    private final DishCategoryResolver dishCategoryResolver;
    private final DishCategoryShelfLifeRepository dishCategoryShelfLifeRepository;
    private final NutritionCalculationService nutritionCalculationService;

    public CookingService(
            RecipeRepository recipeRepository,
            PantryItemRepository pantryItemRepository,
            CookedDishRepository cookedDishRepository,
            DishCategoryResolver dishCategoryResolver,
            DishCategoryShelfLifeRepository dishCategoryShelfLifeRepository,
            NutritionCalculationService nutritionCalculationService) {
        this.recipeRepository = recipeRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.cookedDishRepository = cookedDishRepository;
        this.dishCategoryResolver = dishCategoryResolver;
        this.dishCategoryShelfLifeRepository = dishCategoryShelfLifeRepository;
        this.nutritionCalculationService = nutritionCalculationService;
    }

    private record ScaledIngredient(RecipeIngredientEntity ingredient, BigDecimal scaledQuantity, List<PantryItem> lockedRows) {
    }

    @Transactional
    public CookedDish confirmCooking(Long userId, Long recipeId, BigDecimal actualPortions, String idempotencyKey) {
        // FR-56/AI-15a: resubmitting the same confirmation must not re-deduct or duplicate.
        Optional<CookedDish> existing = cookedDishRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Recipe recipe = recipeRepository.findWithIngredientsById(recipeId)
                .orElseThrow(() -> new NoSuchElementException("Recipe " + recipeId + " not found"));
        if (!recipe.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Recipe " + recipeId + " does not belong to user " + userId);
        }

        BigDecimal factor = actualPortions.divide(
                BigDecimal.valueOf(recipe.getBasePortions()), SCALE_FACTOR_PRECISION, RoundingMode.HALF_UP);

        // FR-53b: lock and check every non-staple ingredient before deducting any of them —
        // NFR-43b's whole point is that the check and the deduction can't interleave with a
        // concurrent cooking confirmation touching the same stock. FR-29: staples aren't
        // tracked in pantry accounting, so they're skipped here entirely, not just at
        // feasibility-check time.
        List<ScaledIngredient> toDeduct = new ArrayList<>();
        List<CookingInfeasibleException.MissingIngredient> missing = new ArrayList<>();
        for (RecipeIngredientEntity ingredient : recipe.getIngredients()) {
            if (ingredient.getProduct().isStaple()) {
                continue;
            }
            BigDecimal scaledQuantity = ingredient.getQuantity().multiply(factor).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
            List<PantryItem> lockedRows = pantryItemRepository
                    .findByUserIdAndProductIdAndUnitAndStatusOrderByExpiresAtAscForUpdate(
                            userId, ingredient.getProduct().getId(), ingredient.getUnit(), PantryItemStatus.ACTIVE);
            BigDecimal available = lockedRows.stream().map(PantryItem::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);

            if (available.compareTo(scaledQuantity) < 0) {
                missing.add(new CookingInfeasibleException.MissingIngredient(
                        ingredient.getProduct().getId(), ingredient.getUnit(), scaledQuantity, available));
            } else {
                toDeduct.add(new ScaledIngredient(ingredient, scaledQuantity, lockedRows));
            }
        }

        // FR-57: nothing is applied unless everything is feasible — throwing here rolls back
        // the whole transaction, releasing the locks acquired above.
        if (!missing.isEmpty()) {
            throw new CookingInfeasibleException(missing);
        }

        for (ScaledIngredient scaled : toDeduct) {
            deductFifo(scaled.lockedRows(), scaled.scaledQuantity());
        }

        DishCategory category = dishCategoryResolver.resolve(recipe);
        DishCategoryShelfLife shelfLife = dishCategoryShelfLifeRepository.findById(category)
                .orElseThrow(() -> new IllegalStateException("No shelf-life reference for category " + category));

        // FR-54/FR-54a/INV-05: nutrition per portion is computed once, from the recipe's own
        // (unscaled) ingredient list divided by its base portions — scaling changes how many
        // portions exist, never what one portion contains.
        Optional<NutritionValues> perPortionNutrition = nutritionPerPortion(recipe);

        LocalDate cookedAt = LocalDate.now();
        CookedDish cookedDish = new CookedDish(
                userId, recipe, category, actualPortions,
                perPortionNutrition.map(NutritionValues::kcal).orElse(null),
                perPortionNutrition.map(NutritionValues::proteinGrams).orElse(null),
                perPortionNutrition.map(NutritionValues::fatGrams).orElse(null),
                perPortionNutrition.map(NutritionValues::carbsGrams).orElse(null),
                cookedAt, cookedAt.plusDays(shelfLife.getShelfLifeDays()), idempotencyKey);
        return cookedDishRepository.save(cookedDish);
    }

    // Same oldest-expiry-first logic as PantryService.consume — kept separate rather than
    // shared: the two loops are small and live in different packages, and the pessimistic-lock
    // variant here is already holding rows PantryService never sees.
    private void deductFifo(List<PantryItem> lockedRowsOldestFirst, BigDecimal quantity) {
        BigDecimal remaining = quantity;
        for (PantryItem item : lockedRowsOldestFirst) {
            if (remaining.signum() == 0) {
                break;
            }
            BigDecimal deducted = item.getQuantity().min(remaining);
            item.setQuantity(item.getQuantity().subtract(deducted));
            if (item.getQuantity().signum() == 0) {
                item.setStatus(PantryItemStatus.CONSUMED);
            }
            remaining = remaining.subtract(deducted);
        }
        pantryItemRepository.saveAll(lockedRowsOldestFirst);
    }

    private Optional<NutritionValues> nutritionPerPortion(Recipe recipe) {
        BigDecimal totalKcal = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;

        for (RecipeIngredientEntity ingredient : recipe.getIngredients()) {
            Optional<NutritionValues> values = nutritionCalculationService.calculate(
                    ingredient.getProduct(), ingredient.getQuantity(), ingredient.getUnit());
            if (values.isEmpty()) {
                // one unknown ingredient makes the whole dish's nutrition unknown, rather than
                // publishing a partial, misleadingly-low number.
                return Optional.empty();
            }
            totalKcal = totalKcal.add(values.get().kcal());
            totalProtein = totalProtein.add(values.get().proteinGrams());
            totalFat = totalFat.add(values.get().fatGrams());
            totalCarbs = totalCarbs.add(values.get().carbsGrams());
        }

        BigDecimal basePortions = BigDecimal.valueOf(recipe.getBasePortions());
        return Optional.of(new NutritionValues(
                divide(totalKcal, basePortions), divide(totalProtein, basePortions),
                divide(totalFat, basePortions), divide(totalCarbs, basePortions)));
    }

    private static BigDecimal divide(BigDecimal total, BigDecimal basePortions) {
        return total.divide(basePortions, NUTRITION_SCALE, RoundingMode.HALF_UP);
    }
}
