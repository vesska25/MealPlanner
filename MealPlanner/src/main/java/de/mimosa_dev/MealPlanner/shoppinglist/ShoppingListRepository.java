package de.mimosa_dev.MealPlanner.shoppinglist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    // PRD 9.1 step 11: the web client's read-only view needs "the current list", but generate()
    // always inserts a new row rather than upserting one — this is the most recent list a user
    // ever generated, not a distinct "active" concept the domain models explicitly. No fetch
    // joins here on purpose: ORDER BY + implicit LIMIT (from "Top") doesn't mix safely with a
    // ToMany fetch join, so this is only ever used to identify *which* list, not to read it.
    Optional<ShoppingList> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    // Explicit JOIN FETCH by a single known id — same reasoning and pattern as
    // RecipeRepository.findWithIngredientsById: a controller reads product.canonicalName after
    // the repository call returns, outside any transaction of its own, so lazy `items`/`product`
    // would throw LazyInitializationException at that point. LEFT (not inner) JOIN FETCH because
    // a list generated with no active suggestion legitimately has zero items.
    @Query("SELECT sl FROM ShoppingList sl LEFT JOIN FETCH sl.items i LEFT JOIN FETCH i.product WHERE sl.id = :id")
    Optional<ShoppingList> findWithItemsById(@Param("id") Long id);
}
