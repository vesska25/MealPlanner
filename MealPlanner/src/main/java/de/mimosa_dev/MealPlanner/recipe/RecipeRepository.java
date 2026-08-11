package de.mimosa_dev.MealPlanner.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // Explicit JOIN FETCH: a cooking confirmation reads a Recipe persisted in an earlier,
    // unrelated transaction, so lazy ingredients/product would throw LazyInitializationException
    // outside that original session (same lesson as PantryItemRepository's fetch query).
    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients i JOIN FETCH i.product WHERE r.id = :id")
    Optional<Recipe> findWithIngredientsById(@Param("id") Long id);
}
