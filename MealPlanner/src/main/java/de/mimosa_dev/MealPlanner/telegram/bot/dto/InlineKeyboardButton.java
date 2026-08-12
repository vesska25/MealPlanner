package de.mimosa_dev.MealPlanner.telegram.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InlineKeyboardButton(String text, @JsonProperty("callback_data") String callbackData) {
}
