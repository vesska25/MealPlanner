package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/** FR-80: {@code /start <code>} consumes a deep-link code and links the account. */
@Component
public class StartCommandHandler implements TelegramCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(StartCommandHandler.class);

    private final MealPlannerApiClient mealPlannerApiClient;
    private final TelegramBotClient botClient;

    public StartCommandHandler(MealPlannerApiClient mealPlannerApiClient, TelegramBotClient botClient) {
        this.mealPlannerApiClient = mealPlannerApiClient;
        this.botClient = botClient;
    }

    @Override
    public boolean supports(String messageText) {
        return messageText.startsWith("/start");
    }

    @Override
    public void handle(TelegramMessage message) {
        String[] parts = message.text().trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            botClient.sendMessage(message.chat().id(),
                    "Welcome! Get your linking code from the Meal Planner website's Account page, "
                            + "then tap the link there (or send /start <code> here).", null);
            return;
        }

        try {
            mealPlannerApiClient.linkTelegram(parts[1].trim(), message.from().id());
            botClient.sendMessage(message.chat().id(), "✅ Linked! You'll get notifications here from now on.", null);
        } catch (RestClientResponseException e) {
            log.info("Telegram link attempt failed for telegramUserId={}: {}", message.from().id(), e.getMessage());
            botClient.sendMessage(message.chat().id(),
                    "❌ That code is invalid or expired. Generate a new one from the website.", null);
        }
    }
}
