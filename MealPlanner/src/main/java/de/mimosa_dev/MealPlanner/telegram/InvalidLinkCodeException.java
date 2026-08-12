package de.mimosa_dev.MealPlanner.telegram;

/** FR-80a: the code doesn't exist, has expired, or has already been used. */
public class InvalidLinkCodeException extends RuntimeException {

    public InvalidLinkCodeException() {
        super("Telegram link code is invalid, expired, or has already been used");
    }
}
