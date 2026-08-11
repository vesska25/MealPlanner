package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;

/**
 * One tool an agent scenario can expose (AI-13). Implementations wrap an existing domain
 * service — they own JSON (de)serialization and error translation, not business logic.
 */
public interface AgentTool {

    String name();

    Tool definition();

    /**
     * State-changing tools go through {@link de.mimosa_dev.MealPlanner.agent.AgentRunner}'s
     * idempotency check (AI-15); read-only tools don't need one.
     */
    boolean stateChanging();

    /**
     * @throws RuntimeException for any recoverable domain failure (AI-21a) — the runner
     *                          catches these and returns them to the model as an observation.
     *                          Only let infrastructure failures (DB down, etc.) propagate as
     *                          unchecked exceptions the runner can't recover from (AI-21b).
     */
    String execute(Long userId, JsonValue input);
}
