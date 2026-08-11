package de.mimosa_dev.MealPlanner.shoppinglist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    // FR-03: ownership scoped in the query itself, not checked after the fact.
    Optional<ShoppingListItem> findByIdAndUserId(Long id, Long userId);
}
