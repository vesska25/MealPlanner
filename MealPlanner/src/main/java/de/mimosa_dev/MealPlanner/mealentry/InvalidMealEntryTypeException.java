package de.mimosa_dev.MealPlanner.mealentry;

/** FR-50a/INV-13: COOKED_DISH entries can only ever be created as a cooking-confirmation side effect. */
public class InvalidMealEntryTypeException extends RuntimeException {

    public InvalidMealEntryTypeException(String message) {
        super(message);
    }
}
