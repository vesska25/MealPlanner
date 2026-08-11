package de.mimosa_dev.MealPlanner.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Email " + email + " is already registered");
    }
}
