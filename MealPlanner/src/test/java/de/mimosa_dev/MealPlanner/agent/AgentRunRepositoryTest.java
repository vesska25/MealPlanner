package de.mimosa_dev.MealPlanner.agent;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatCode;

class AgentRunRepositoryTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    // The DB's chk_agent_run_scenario CHECK constraint is hand-maintained alongside the
    // AgentScenario enum (each new scenario needs its own migration to widen the constraint —
    // see V6, V11) rather than derived from it, so nothing stops the two from drifting apart.
    // This is the cheap way to catch that drift: no LLM call, no full agent loop, just the
    // persistence step every real AgentRunner.run() does — SHOPPING_LIST (added in step 10
    // Phase B) went unnoticed by every other test because they all mock AgentRunner rather than
    // exercising a real run against Postgres.
    @ParameterizedTest
    @EnumSource(AgentScenario.class)
    void everyScenarioCanBePersisted(AgentScenario scenario) {
        assertThatCode(() -> agentRunRepository.saveAndFlush(
                new AgentRun(USER_ID, scenario, "test", "prompt-hash")))
                .doesNotThrowAnyException();
    }
}
