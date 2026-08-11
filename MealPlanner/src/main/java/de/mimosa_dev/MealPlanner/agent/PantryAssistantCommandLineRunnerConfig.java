package de.mimosa_dev.MealPlanner.agent;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Manual entry point for exercising the real pantry-assistant agent against the live API. Run
 * with: {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=pantry-assistant}
 * Not wired into the test suite — it makes a real, billed call to the Anthropic API.
 */
@Configuration
@Profile("pantry-assistant")
class PantryAssistantCommandLineRunnerConfig {

    @Bean
    CommandLineRunner runPantryAssistant(AgentRunner runner) {
        return args -> {
            String question = args.length > 0 ? String.join(" ", args) : "What's in my pantry?";
            AgentRunOutcome outcome = runner.run(1L, AgentScenario.PANTRY_ASSISTANT, "manual_cli", question);
            System.out.println("[" + outcome.status() + "] " + outcome.message());
        };
    }
}
