package de.mimosa_dev.MealPlanner.telegram.controller;

import de.mimosa_dev.MealPlanner.telegram.InvalidLinkCodeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = TelegramController.class)
public class TelegramExceptionHandler {

    @ExceptionHandler(InvalidLinkCodeException.class)
    public ResponseEntity<String> handleInvalidLinkCode(InvalidLinkCodeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
