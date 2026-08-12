package de.mimosa_dev.MealPlanner.cooking.controller;

import de.mimosa_dev.MealPlanner.cooking.CookingInfeasibleException;
import de.mimosa_dev.MealPlanner.cooking.InsufficientPortionsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.math.BigDecimal;
import java.util.List;

@RestControllerAdvice(basePackageClasses = CookingController.class)
public class CookingExceptionHandler {

    public record MissingIngredientResponse(Long productId, String unit, BigDecimal needed, BigDecimal available) {
    }

    public record CookingInfeasibleResponse(String message, List<MissingIngredientResponse> missingIngredients) {
    }

    @ExceptionHandler(CookingInfeasibleException.class)
    public ResponseEntity<CookingInfeasibleResponse> handleInfeasible(CookingInfeasibleException e) {
        var missing = e.missingIngredients().stream()
                .map(m -> new MissingIngredientResponse(m.productId(), m.unit().name(), m.needed(), m.available()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CookingInfeasibleResponse(e.getMessage(), missing));
    }

    @ExceptionHandler(InsufficientPortionsException.class)
    public ResponseEntity<String> handleInsufficientPortions(InsufficientPortionsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
