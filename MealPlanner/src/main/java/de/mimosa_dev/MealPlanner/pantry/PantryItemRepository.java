package de.mimosa_dev.MealPlanner.pantry;

import de.mimosa_dev.MealPlanner.common.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    List<PantryItem> findByUserIdAndProductIdAndUnitAndStatusOrderByExpiresAtAsc(
            Long userId, Long productId, Unit unit, PantryItemStatus status);

    // Explicit JOIN FETCH, not a derived query: callers (e.g. GetPantryContentsTool) read
    // product.canonicalName after the repository call returns, often outside any transaction —
    // a lazy `product` association would throw LazyInitializationException at that point.
    @Query("SELECT pi FROM PantryItem pi JOIN FETCH pi.product "
            + "WHERE pi.userId = :userId AND pi.status = :status ORDER BY pi.expiresAt ASC")
    List<PantryItem> findByUserIdAndStatusOrderByExpiresAtAsc(
            @Param("userId") Long userId, @Param("status") PantryItemStatus status);

    @Query("SELECT COALESCE(SUM(pi.quantity), 0) FROM PantryItem pi "
            + "WHERE pi.userId = :userId AND pi.product.id = :productId "
            + "AND pi.unit = :unit AND pi.status = :status")
    BigDecimal sumQuantityByUserAndProductAndUnitAndStatus(
            @Param("userId") Long userId, @Param("productId") Long productId,
            @Param("unit") Unit unit, @Param("status") PantryItemStatus status);
}
