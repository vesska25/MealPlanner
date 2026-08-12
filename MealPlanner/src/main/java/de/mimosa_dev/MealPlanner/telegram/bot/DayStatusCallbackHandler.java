package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.auth.JwtService;
import de.mimosa_dev.MealPlanner.mealentry.MealEntryType;
import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramCallbackQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/** {@code callback_data} format {@code day:<OUTSIDE|GUEST|SKIPPED>}, from {@link DayCommandHandler}'s keyboard. */
@Component
public class DayStatusCallbackHandler implements TelegramCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(DayStatusCallbackHandler.class);

    private final TelegramLinkService telegramLinkService;
    private final JwtService jwtService;
    private final MealPlannerApiClient mealPlannerApiClient;
    private final TelegramBotClient botClient;

    public DayStatusCallbackHandler(
            TelegramLinkService telegramLinkService, JwtService jwtService,
            MealPlannerApiClient mealPlannerApiClient, TelegramBotClient botClient) {
        this.telegramLinkService = telegramLinkService;
        this.jwtService = jwtService;
        this.mealPlannerApiClient = mealPlannerApiClient;
        this.botClient = botClient;
    }

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith("day:");
    }

    @Override
    public void handle(TelegramCallbackQuery callback) {
        var userId = telegramLinkService.findUserIdByTelegramUserId(callback.from().id());
        if (userId.isEmpty()) {
            botClient.answerCallbackQuery(callback.id(), "Please link your account first.");
            return;
        }

        MealEntryType type = MealEntryType.valueOf(callback.data().substring("day:".length()));

        try {
            String jwt = jwtService.generate(userId.get());
            mealPlannerApiClient.createMealEntry(jwt, type);
            botClient.answerCallbackQuery(callback.id(), "Got it, thanks!");
        } catch (RestClientResponseException e) {
            log.info("Day-status callback failed for userId={}: {}", userId.get(), e.getMessage());
            botClient.answerCallbackQuery(callback.id(), "❌ Couldn't record that — " + e.getStatusCode());
        }
    }
}
