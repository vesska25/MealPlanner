package de.mimosa_dev.MealPlanner.telegram.dto;

import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;

import java.time.Instant;

public record GenerateLinkCodeResponse(String code, String deepLink, Instant expiresAt) {

    public static GenerateLinkCodeResponse from(TelegramLinkService.GeneratedLinkCode generated) {
        return new GenerateLinkCodeResponse(generated.code(), generated.deepLink(), generated.expiresAt());
    }
}
