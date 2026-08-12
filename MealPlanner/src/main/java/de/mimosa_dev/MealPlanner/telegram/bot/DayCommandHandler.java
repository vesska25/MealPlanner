package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.InlineKeyboardButton;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.InlineKeyboardMarkup;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramMessage;
import org.springframework.stereotype.Component;

/**
 * FR-82's "day status" via inline buttons — user-initiated, since FR-81's three notification
 * types don't include "ask about your day" (no proactive trigger exists for this).
 */
@Component
public class DayCommandHandler implements TelegramCommandHandler {

    private final TelegramLinkService telegramLinkService;
    private final TelegramBotClient botClient;

    public DayCommandHandler(TelegramLinkService telegramLinkService, TelegramBotClient botClient) {
        this.telegramLinkService = telegramLinkService;
        this.botClient = botClient;
    }

    @Override
    public boolean supports(String messageText) {
        return messageText.equals("/day");
    }

    @Override
    public void handle(TelegramMessage message) {
        if (telegramLinkService.findUserIdByTelegramUserId(message.from().id()).isEmpty()) {
            botClient.sendMessage(message.chat().id(), "Please link your account first — see /start.", null);
            return;
        }

        var keyboard = new InlineKeyboardMarkup(java.util.List.of(java.util.List.of(
                new InlineKeyboardButton("🍽 Ate out", "day:OUTSIDE"),
                new InlineKeyboardButton("👥 At a friend's", "day:GUEST"),
                new InlineKeyboardButton("😔 Skipped", "day:SKIPPED"))));
        botClient.sendMessage(message.chat().id(), "How did today's meal go?", keyboard);
    }
}
