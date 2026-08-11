package de.mimosa_dev.MealPlanner.auth;

/** FR-01: the invite code doesn't exist, or has already been used. */
public class InvalidInviteCodeException extends RuntimeException {

    public InvalidInviteCodeException() {
        super("Invite code is invalid or has already been used");
    }
}
