package de.mimosa_dev.MealPlanner.agentspike;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final long MAX_TOKENS = 1024L;

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
     * @param userMessage a natural-language question the model can answer by calling
     *                    {@link ProductLookupTool}, e.g. "what dairy products do we have?"
     * @return the model's final text response
     */
    String run(String userMessage) {
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(userMessage)
                .build());

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            Message response = client.messages().create(MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(MAX_TOKENS)
                    .addTool(productLookupTool.definition())
                    .messages(messages)
                    .build());

            StopReason stopReason = response.stopReason()
                    .orElseThrow(() -> new IllegalStateException("Response is missing stop_reason"));

            // The model may emit tool_use alongside text (e.g. "Let me check that."), so the
            // assistant turn must echo every block back, not just the tool_use ones (AI-07:
            // the model's own claim carries no weight, but its message history still does).
            List<ContentBlockParam> assistantContent = new ArrayList<>();
            List<ContentBlockParam> toolResults = new ArrayList<>();
            for (ContentBlock block : response.content()) {
                assistantContent.add(block.toParam());
                block.toolUse().ifPresent(toolUse -> toolResults.add(ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder()
                                .toolUseId(toolUse.id())
                                .content(executeTool(toolUse))
                                .build())));
            }
            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.ASSISTANT)
                    .contentOfBlockParams(assistantContent)
                    .build());

            if (!StopReason.TOOL_USE.equals(stopReason)) {
                return response.content().stream()
                        .flatMap(block -> block.text().stream())
                        .map(TextBlock::text)
                        .collect(Collectors.joining("\n"));
            }

            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(toolResults)
                    .build());
        }

        throw new IllegalStateException(
                "Tool-calling loop exceeded " + MAX_ITERATIONS + " iterations without a final response");
    }

    private String executeTool(ToolUseBlock toolUse) {
        if (!ProductLookupTool.NAME.equals(toolUse.name())) {
            throw new IllegalStateException("Unknown tool requested: " + toolUse.name());
        }
        LookupProductsInput input = toolUse._input().convert(LookupProductsInput.class);
        return productLookupTool.execute(input.query());
    }

    private record LookupProductsInput(String query) {
    }
}
