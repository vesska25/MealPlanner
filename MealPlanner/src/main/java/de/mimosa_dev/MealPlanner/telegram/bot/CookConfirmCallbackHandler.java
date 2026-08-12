package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.auth.JwtService;
import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramCallbackQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/**
 * FR-82's "cooking confirmation" via inline buttons — {@code callback_data} format
 * {@code cook:<recipeId>:<basePortions>}, sent by {@code SuggestionActivatedEventListener}
 * (step 12 Phase C). AC#21: the idempotency key is Telegram's own {@code callback_query.id} —
 * deterministic per callback, so Telegram's own redelivery of the same callback produces the
 * same key and hits {@code CookingService.confirmCooking}'s existing short-circuit for free.
 */
@Component
public class CookConfirmCallbackHandler implements TelegramCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(CookConfirmCallbackHandler.class);

    private final TelegramLinkService telegramLinkService;
    private final JwtService jwtService;
    private final MealPlannerApiClient mealPlannerApiClient;
    private final TelegramBotClient botClient;

    public CookConfirmCallbackHandler(
            TelegramLinkService telegramLinkService, JwtService jwtService,
            MealPlannerApiClient mealPlannerApiClient, TelegramBotClient botClient) {
        this.telegramLinkService = telegramLinkService;
        this.jwtService = jwtService;
        this.mealPlannerApiClient = mealPlannerApiClient;
        this.botClient = botClient;
    }

    @Override
    public boolean supports(String callbackData) {
        return callbackData != null && callbackData.startsWith("cook:");
    }

    @Override
    public void handle(TelegramCallbackQuery callback) {
        var userId = telegramLinkService.findUserIdByTelegramUserId(callback.from().id());
        if (userId.isEmpty()) {
            botClient.answerCallbackQuery(callback.id(), "Please link your account first.");
            return;
        }

        String[] parts = callback.data().split(":");
        Long recipeId = Long.valueOf(parts[1]);
        BigDecimal basePortions = new BigDecimal(parts[2]);

        try {
            String jwt = jwtService.generate(userId.get());
            mealPlannerApiClient.confirmCooking(jwt, recipeId, basePortions, callback.id());
            botClient.answerCallbackQuery(callback.id(), "✅ Cooking confirmed!");
        } catch (RestClientResponseException e) {
            log.info("Cook-confirm callback failed for userId={}: {}", userId.get(), e.getMessage());
            botClient.answerCallbackQuery(callback.id(), "❌ Couldn't confirm — " + e.getStatusCode());
        }
    }
}
