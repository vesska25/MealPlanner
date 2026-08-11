package de.mimosa_dev.MealPlanner.agentspike;

import com.anthropic.client.AnthropicClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Throwaway agent-layer spike (PRD 9.1 step 3): one read tool, one model, a manual
 * tool-calling loop. The goal is to confirm this shape works on the Anthropic Java SDK
 * before building the real deterministic services (step 4), the hard-constraint validator
 * (step 5), and the agent layer proper (step 6) on top of it. The output of running this is
 * discarded once verified — this class isn't meant to survive into the real agent layer.
 */
@Component
class AgentSpikeRunner {

    private static final int MAX_ITERATIONS = 5; // mirrors AI-20's default tool-calling iteration cap

    private final AnthropicClient client;
    private final ProductLookupTool productLookupTool;
    private final String model;

    AgentSpikeRunner(
            AnthropicClient client,
            ProductLookupTool productLookupTool,
            @Value("${anthropic.model}") String model) {
        this.client = client;
        this.productLookupTool = productLookupTool;
        this.model = model;
    }

    /**
     * Sends {@code userMessage} to the model with {@link ProductLookupTool#definition()} as
     * its only available tool, and drives the tool-calling loop to a final text response.
     *
     * <p>Must handle:
     * <ul>
     *   <li>Sending the initial request with the tool attached, then appending the model's
     *       response to the running message history before continuing — required so the
     *       model sees its own prior {@code tool_use} blocks on the next turn</li>
     *   <li>Inspecting {@code stop_reason}: on {@code tool_use}, extract the tool_use
     *       block(s), call {@link ProductLookupTool#execute(String)} with the model-supplied
     *       {@code query} input, and send the result(s) back as {@code tool_result} content
     *       block(s) in a new user turn</li>
     *   <li>Looping until {@code stop_reason} is {@code end_turn}, or {@link #MAX_ITERATIONS}
     *       is reached (AI-20) — at which point this spike can just throw, since the real
     *       fallback (AI-20a) doesn't exist yet</li>
     *   <li>Returning the final assistant text response for manual inspection</li>
     * </ul>
     *
     * @param userMessage a natural-language question the model can answer by calling
     *                    {@link ProductLookupTool}, e.g. "what dairy products do we have?"
     * @return the model's final text response
     */
    String run(String userMessage) {
        // TODO(sergio): implement — see PRD 9.1 step 3
        throw new UnsupportedOperationException("not implemented yet");
    }
}
