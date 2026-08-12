package de.mimosa_dev.MealPlanner.telegram.bot.dto;

/** Every Telegram Bot API response envelope. */
public record TelegramApiResponse<T>(boolean ok, T result) {
}
