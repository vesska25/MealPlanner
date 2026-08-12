package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramMessage;

public interface TelegramCommandHandler {

    boolean supports(String messageText);

    void handle(TelegramMessage message);
}
