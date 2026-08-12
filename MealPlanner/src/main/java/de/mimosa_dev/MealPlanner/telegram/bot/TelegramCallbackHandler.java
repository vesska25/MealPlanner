package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramCallbackQuery;

public interface TelegramCallbackHandler {

    boolean supports(String callbackData);

    void handle(TelegramCallbackQuery callback);
}
