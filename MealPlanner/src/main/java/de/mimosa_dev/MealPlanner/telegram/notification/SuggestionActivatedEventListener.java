package de.mimosa_dev.MealPlanner.telegram.notification;

import de.mimosa_dev.MealPlanner.recipe.SuggestionActivatedEvent;
import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;
import de.mimosa_dev.MealPlanner.telegram.bot.TelegramBotClient;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.InlineKeyboardButton;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.InlineKeyboardMarkup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * FR-81 notification type 1 ("a new suggestion is ready") — event-driven, not polled: fires
 * exactly once per real {@code RecipeSuggestionService.activate(...)} call, which already
 * expires any prior suggestion first, so there's structurally never a duplicate to dedup. Uses
 * {@code AFTER_COMMIT} so a suggestion that ends up rolled back never triggers a notification.
 */
@Component
@ConditionalOnProperty(name = "telegram.bot-token")
public class SuggestionActivatedEventListener {

    private final TelegramLinkService telegramLinkService;
    private final TelegramBotClient botClient;

    public SuggestionActivatedEventListener(TelegramLinkService telegramLinkService, TelegramBotClient botClient) {
        this.telegramLinkService = telegramLinkService;
        this.botClient = botClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSuggestionActivated(SuggestionActivatedEvent event) {
        telegramLinkService.findLinkByUserId(event.userId()).ifPresent(link -> {
            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.singleRow(
                    new InlineKeyboardButton("✅ I cooked it", "cook:" + event.recipeId() + ":" + event.basePortions()));
            botClient.sendMessage(link.getTelegramUserId(), "🍳 New suggestion: " + event.recipeName(), keyboard);
        });
    }
}
