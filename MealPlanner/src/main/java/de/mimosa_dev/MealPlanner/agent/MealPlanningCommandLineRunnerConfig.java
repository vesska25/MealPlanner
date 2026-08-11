package de.mimosa_dev.MealPlanner.agent;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Manual entry point for exercising the real meal-planning agent against the live API. Run
 * with: {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=meal-planning}
 * Not wired into the test suite — it makes a real, billed call to the Anthropic API.
 */
@Configuration
@Profile("meal-planning")
class MealPlanningCommandLineRunnerConfig {

    @Bean
    CommandLineRunner runMealPlanning(AgentRunner runner) {
        return args -> {
            String question = args.length > 0 ? String.join(" ", args) : "What should I cook today?";
            AgentRunOutcome outcome = runner.run(1L, AgentScenario.MEAL_PLANNING, "manual_cli", question);
            System.out.println("[" + outcome.status() + "] " + outcome.message());
        };
    }
}
