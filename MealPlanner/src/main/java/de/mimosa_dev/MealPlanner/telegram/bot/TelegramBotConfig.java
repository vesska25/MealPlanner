package de.mimosa_dev.MealPlanner.telegram.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Two separate {@link RestClient}s, deliberately: one talks to Telegram's Bot API (needs a read
 * timeout comfortably longer than the 30s long-poll timeout {@link TelegramLongPollingRunner}
 * uses, or every poll would abort as a timeout instead of a legitimate empty response), the
 * other talks to our own backend (short default timeout is fine — see {@link MealPlannerApiClient}).
 */
@Configuration
public class TelegramBotConfig {

    @Bean
    public RestClient telegramApiRestClient(@Value("${telegram.bot-token:}") String botToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(35));
        return RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RestClient internalApiRestClient(@Value("${telegram.internal-api-base-url:http://localhost:8080}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
