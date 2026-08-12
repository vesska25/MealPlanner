package de.mimosa_dev.MealPlanner.telegram.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramMessage(
        @JsonProperty("message_id") long messageId, TelegramChat chat, TelegramUser from, String text) {
}
