package de.mimosa_dev.MealPlanner.cooking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CookedDishRepository extends JpaRepository<CookedDish, Long> {

    // FR-56/AI-15a: cooking confirmation is idempotent via an explicitly supplied key —
    // CookingService checks this before doing anything else.
    Optional<CookedDish> findByIdempotencyKey(String idempotencyKey);

    // FR-04 (full data export).
    List<CookedDish> findByUserId(Long userId);

    // FR-33: matched by recipe name (same Phase A simplification as PreferenceSignal) rather
    // than recipe id, since a fresh model-generated candidate has no id to compare against yet.
    @Query("SELECT COUNT(cd) > 0 FROM CookedDish cd WHERE cd.userId = :userId "
            + "AND LOWER(cd.recipe.name) = LOWER(:name) AND cd.cookedAt > :after")
    boolean existsRecentlyCookedByName(
            @Param("userId") Long userId, @Param("name") String name, @Param("after") LocalDate after);
}
