package de.mimosa_dev.MealPlanner.telegram.dto;

import de.mimosa_dev.MealPlanner.telegram.TelegramLink;

import java.time.Instant;

public record TelegramLinkStatusResponse(boolean linked, Long telegramUserId, Instant linkedAt) {

    public static TelegramLinkStatusResponse notLinked() {
        return new TelegramLinkStatusResponse(false, null, null);
    }

    public static TelegramLinkStatusResponse from(TelegramLink link) {
        return new TelegramLinkStatusResponse(true, link.getTelegramUserId(), link.getLinkedAt());
    }
}
