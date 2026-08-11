package de.mimosa_dev.MealPlanner.pantry;

import de.mimosa_dev.MealPlanner.common.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    List<PantryItem> findByUserIdAndProductIdAndUnitAndStatusOrderByExpiresAtAsc(
            Long userId, Long productId, Unit unit, PantryItemStatus status);
}
