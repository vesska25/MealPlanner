package de.mimosa_dev.MealPlanner.cooking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

/**
 * Operations on the cooked-portions inventory (FR-52b: "the same operations — consume, expire,
 * discard — apply to both entities" as {@code pantry_item}). Mirrors
 * {@link de.mimosa_dev.MealPlanner.pantry.PantryService}'s discard guard (INV-02's equivalent
 * here: a non-{@code ACTIVE} cooked dish can't be consumed or discarded again).
 */
@Service
public class CookedDishService {

    private final CookedDishRepository cookedDishRepository;

    public CookedDishService(CookedDishRepository cookedDishRepository) {
        this.cookedDishRepository = cookedDishRepository;
    }

    /**
     * FR-52c: eaten portions support fractional values; the remainder is reduced by the amount
     * eaten, and reaching zero flips the record to {@code CONSUMED}.
     */
    @Transactional
    public void consumePortions(Long cookedDishId, BigDecimal portionsEaten) {
        if (portionsEaten.signum() <= 0) {
            throw new IllegalArgumentException("Portions eaten must be positive, got " + portionsEaten);
        }

        CookedDish dish = cookedDishRepository.findById(cookedDishId)
                .orElseThrow(() -> new NoSuchElementException("Cooked dish " + cookedDishId + " not found"));
        if (dish.getStatus() != CookedDishStatus.ACTIVE) {
            throw new IllegalStateException("Cooked dish " + cookedDishId + " is " + dish.getStatus() + ", not ACTIVE");
        }
        if (portionsEaten.compareTo(dish.getPortionsRemaining()) > 0) {
            throw new InsufficientPortionsException(cookedDishId, portionsEaten, dish.getPortionsRemaining());
        }

        BigDecimal remaining = dish.getPortionsRemaining().subtract(portionsEaten);
        dish.setPortionsRemaining(remaining);
        if (remaining.signum() == 0) {
            dish.setStatus(CookedDishStatus.CONSUMED);
        }
        cookedDishRepository.save(dish);
    }

    /** No discard-reason enum here — unlike pantry_item's FR-23, the PRD never enumerates one. */
    @Transactional
    public void discard(Long cookedDishId) {
        CookedDish dish = cookedDishRepository.findById(cookedDishId)
                .orElseThrow(() -> new NoSuchElementException("Cooked dish " + cookedDishId + " not found"));
        if (dish.getStatus() != CookedDishStatus.ACTIVE) {
            throw new IllegalStateException("Cooked dish " + cookedDishId + " is " + dish.getStatus() + ", not ACTIVE");
        }
        dish.setStatus(CookedDishStatus.DISCARDED);
        cookedDishRepository.save(dish);
    }
}
