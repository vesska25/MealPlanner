package de.mimosa_dev.MealPlanner.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeSuggestionRepository extends JpaRepository<RecipeSuggestion, Long> {

    Optional<RecipeSuggestion> findByUserIdAndStatus(Long userId, RecipeSuggestionStatus status);

    Optional<RecipeSuggestion> findByUserIdAndRecipeIdAndStatus(Long userId, Long recipeId, RecipeSuggestionStatus status);
}
