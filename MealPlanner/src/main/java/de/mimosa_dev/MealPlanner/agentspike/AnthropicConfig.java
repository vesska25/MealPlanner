package de.mimosa_dev.MealPlanner.agentspike;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider client for the throwaway agent-layer spike (PRD 9.1 step 3). The model/provider
 * being configuration rather than hardcoded (AI-41) is worth keeping even in the spike,
 * since it's the pattern the real agent layer reuses in step 6.
 */
@Configuration
class AnthropicConfig {

    @Bean
    AnthropicClient anthropicClient(@Value("${anthropic.api-key}") String apiKey) {
        return AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }
}
