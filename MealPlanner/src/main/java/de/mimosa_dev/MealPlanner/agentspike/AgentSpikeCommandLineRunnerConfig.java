package de.mimosa_dev.MealPlanner.agentspike;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Manual entry point for the throwaway spike (PRD 9.1 step 3). Run with:
 * {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=agent-spike}
 * Not wired into the test suite — it makes a real, billed call to the Anthropic API.
 */
@Configuration
@Profile("agent-spike")
class AgentSpikeCommandLineRunnerConfig {

    @Bean
    CommandLineRunner runSpike(AgentSpikeRunner runner) {
        return args -> {
            String question = args.length > 0 ? String.join(" ", args) : "What dairy products do we have?";
            System.out.println(runner.run(question));
        };
    }
}
