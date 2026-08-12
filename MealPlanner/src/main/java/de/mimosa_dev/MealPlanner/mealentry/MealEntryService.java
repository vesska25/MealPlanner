package de.mimosa_dev.MealPlanner.mealentry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * FR-50/FR-50a (PRD step 12). {@link #recordCookedDish} is called only from
 * {@code CookingService.confirmCooking} as a side effect — never accepted as client input,
 * which is what guarantees INV-13 by construction rather than by trusting a caller-supplied
 * dish reference.
 */
@Service
public class MealEntryService {

    private final MealEntryRepository mealEntryRepository;

    public MealEntryService(MealEntryRepository mealEntryRepository) {
        this.mealEntryRepository = mealEntryRepository;
    }

    @Transactional
    public MealEntry recordCookedDish(Long userId, Long cookedDishId, BigDecimal portionsEaten) {
        return mealEntryRepository.save(MealEntry.cookedDish(userId, cookedDishId, portionsEaten));
    }

    /**
     * REST-facing (FR-82's "day status" bot buttons, and the same endpoint for a web client).
     * Only the 3 non-cooked types are accepted — a client can never legitimately supply a valid
     * {@code cookedDishId}, so COOKED_DISH is rejected outright rather than silently ignored.
     */
    @Transactional
    public MealEntry recordOther(
            Long userId, MealEntryType type, BigDecimal kcal, BigDecimal proteinGrams, BigDecimal fatGrams, BigDecimal carbsGrams) {
        if (type == MealEntryType.COOKED_DISH) {
            throw new InvalidMealEntryTypeException(
                    "COOKED_DISH meal entries are created automatically by cooking confirmation, not via this endpoint");
        }
        return mealEntryRepository.save(MealEntry.other(userId, type, kcal, proteinGrams, fatGrams, carbsGrams));
    }
}
