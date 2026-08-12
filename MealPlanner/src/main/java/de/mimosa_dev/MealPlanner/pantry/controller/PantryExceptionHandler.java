package de.mimosa_dev.MealPlanner.pantry.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * {@code PantryService.discard} throws a plain {@link IllegalStateException} for INV-02 (can't
 * discard an already consumed/discarded item) — without this it would fall through to the
 * default Spring 500, same class of gap {@code CookingExceptionHandler} closed for the cooking
 * package's domain exceptions.
 */
@RestControllerAdvice(basePackageClasses = PantryController.class)
public class PantryExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
