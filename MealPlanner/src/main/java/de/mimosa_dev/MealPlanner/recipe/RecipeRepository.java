package de.mimosa_dev.MealPlanner.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // Explicit JOIN FETCH: a cooking confirmation reads a Recipe persisted in an earlier,
    // unrelated transaction, so lazy ingredients/product would throw LazyInitializationException
    // outside that original session (same lesson as PantryItemRepository's fetch query).
    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients i JOIN FETCH i.product WHERE r.id = :id")
    Optional<Recipe> findWithIngredientsById(@Param("id") Long id);

    // FR-04 (full data export). No fetch join here (unlike findWithIngredientsById) — the
    // caller (AccountService) stays within one @Transactional method, so lazily loading each
    // recipe's ingredients afterward is fine and avoids the row-multiplication a collection
    // JOIN FETCH would need DISTINCT to work around.
    List<Recipe> findByUserId(Long userId);
}
