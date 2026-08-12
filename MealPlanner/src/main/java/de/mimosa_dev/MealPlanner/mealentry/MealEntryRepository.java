package de.mimosa_dev.MealPlanner.mealentry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {
}
