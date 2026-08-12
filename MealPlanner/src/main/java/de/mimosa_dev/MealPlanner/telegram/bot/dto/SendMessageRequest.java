package de.mimosa_dev.MealPlanner.telegram.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendMessageRequest(
        @JsonProperty("chat_id") long chatId, String text, @JsonProperty("reply_markup") InlineKeyboardMarkup replyMarkup) {
}
