package de.mimosa_dev.MealPlanner.mealentry.controller;

import de.mimosa_dev.MealPlanner.mealentry.InvalidMealEntryTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = MealEntryController.class)
public class MealEntryExceptionHandler {

    @ExceptionHandler(InvalidMealEntryTypeException.class)
    public ResponseEntity<String> handleInvalidType(InvalidMealEntryTypeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
