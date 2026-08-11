package de.mimosa_dev.MealPlanner.cooking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CookedDishRepository extends JpaRepository<CookedDish, Long> {

    // FR-56/AI-15a: cooking confirmation is idempotent via an explicitly supplied key —
    // CookingService checks this before doing anything else.
    Optional<CookedDish> findByIdempotencyKey(String idempotencyKey);
}
