package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.bot.dto.AnswerCallbackQueryRequest;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.InlineKeyboardMarkup;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.SendMessageRequest;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramApiResponse;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramUpdate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * The only three Telegram Bot API calls this bot needs (FR-83: no business logic here, just raw
 * transport — decisions live in the handler classes).
 */
@Component
public class TelegramBotClient {

    private final RestClient telegramApiRestClient;

    public TelegramBotClient(@Qualifier("telegramApiRestClient") RestClient telegramApiRestClient) {
        this.telegramApiRestClient = telegramApiRestClient;
    }

    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) {
        TelegramApiResponse<List<TelegramUpdate>> response = telegramApiRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/getUpdates")
                        .queryParam("offset", offset)
                        .queryParam("timeout", timeoutSeconds)
                        .queryParam("allowed_updates", "[\"message\",\"callback_query\"]")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<TelegramApiResponse<List<TelegramUpdate>>>() {
                });
        return response == null || response.result() == null ? List.of() : response.result();
    }

    public void sendMessage(long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        telegramApiRestClient.post()
                .uri("/sendMessage")
                .body(new SendMessageRequest(chatId, text, replyMarkup))
                .retrieve()
                .toBodilessEntity();
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        telegramApiRestClient.post()
                .uri("/answerCallbackQuery")
                .body(new AnswerCallbackQueryRequest(callbackQueryId, text))
                .retrieve()
                .toBodilessEntity();
    }
}
