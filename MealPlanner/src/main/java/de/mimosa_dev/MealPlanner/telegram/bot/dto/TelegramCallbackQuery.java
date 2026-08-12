package de.mimosa_dev.MealPlanner.telegram.bot.dto;

public record TelegramCallbackQuery(String id, TelegramUser from, TelegramMessage message, String data) {
}
