package de.mimosa_dev.MealPlanner.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Applies to every controller (unlike {@code auth.controller.AuthExceptionHandler}, which is
 * scoped to the auth package). A {@code NoSuchElementException} — "not found, or not yours"
 * throughout this codebase (see the FR-03 ownership checks in PantryService/CookedDishService)
 * — becomes a plain 404 rather than leaking a default Spring Boot error body with an internal
 * exception message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.notFound().build();
    }
}
