package de.mimosa_dev.MealPlanner.auth.controller;

import de.mimosa_dev.MealPlanner.auth.EmailAlreadyRegisteredException;
import de.mimosa_dev.MealPlanner.auth.InvalidCredentialsException;
import de.mimosa_dev.MealPlanner.auth.InvalidInviteCodeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler({InvalidInviteCodeException.class, EmailAlreadyRegisteredException.class})
    public ResponseEntity<String> handleBadRegistration(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
}
