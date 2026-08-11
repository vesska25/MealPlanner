package de.mimosa_dev.MealPlanner.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceSignalRepository extends JpaRepository<PreferenceSignal, Long> {

    boolean existsByUserIdAndRecipeNameIgnoreCase(Long userId, String recipeName);
}
