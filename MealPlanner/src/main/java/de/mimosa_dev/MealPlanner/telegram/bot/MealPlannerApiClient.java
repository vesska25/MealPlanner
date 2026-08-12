package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.cooking.dto.CookedDishResponse;
import de.mimosa_dev.MealPlanner.cooking.dto.ConfirmCookingRequest;
import de.mimosa_dev.MealPlanner.mealentry.MealEntryType;
import de.mimosa_dev.MealPlanner.mealentry.dto.CreateMealEntryRequest;
import de.mimosa_dev.MealPlanner.telegram.dto.LinkTelegramRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * FR-83's literal expression: the bot calls the SAME REST endpoints the web client does,
 * reusing the exact same request/response record types rather than inventing bot-specific ones.
 * Callers mint a short-lived JWT via the existing {@code JwtService.generate(userId)} — Telegram's
 * own long-polling channel (authenticated by bot-token possession) is what establishes that a
 * request really came from the linked telegramUserId, so no separate credential check is needed
 * here.
 */
@Component
public class MealPlannerApiClient {

    private final RestClient internalApiRestClient;

    public MealPlannerApiClient(@Qualifier("internalApiRestClient") RestClient internalApiRestClient) {
        this.internalApiRestClient = internalApiRestClient;
    }

    public void linkTelegram(String code, long telegramUserId) {
        internalApiRestClient.post()
                .uri("/api/telegram/link")
                .body(new LinkTelegramRequest(code, telegramUserId))
                .retrieve()
                .toBodilessEntity();
    }

    public CookedDishResponse confirmCooking(String jwt, Long recipeId, BigDecimal actualPortions, String idempotencyKey) {
        return internalApiRestClient.post()
                .uri("/api/cooking/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .body(new ConfirmCookingRequest(recipeId, actualPortions, idempotencyKey))
                .retrieve()
                .body(CookedDishResponse.class);
    }

    public void createMealEntry(String jwt, MealEntryType type) {
        internalApiRestClient.post()
                .uri("/api/meal-entries")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .body(new CreateMealEntryRequest(type, null, null, null, null))
                .retrieve()
                .toBodilessEntity();
    }
}
