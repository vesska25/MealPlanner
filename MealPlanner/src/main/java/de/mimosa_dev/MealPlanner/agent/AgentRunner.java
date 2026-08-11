package de.mimosa_dev.MealPlanner.agent;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import de.mimosa_dev.MealPlanner.agent.tool.AgentTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The tool-calling loop (PRD 9.1 step 6; PRD section 4.6's state machine). Every run is logged
 * (AI-30/31), every tool call is scoped to the run's scenario (AI-13/AI-14), and the model is
 * never trusted as the source of truth for anything a tool didn't confirm (AI-07). Enforcement
 * here is structural — a tool the scenario doesn't expose is never sent to the model in the
 * first place, not merely disallowed after the fact.
 */
@Service
public class AgentRunner {

    private static final int MAX_ITERATIONS = 5; // AI-20 default
    private static final long MAX_TOKENS = 1024L;

    private final AnthropicClient client;
    private final ScenarioToolProvider toolProvider;
    private final AgentRunRepository agentRunRepository;
    private final ToolCallRepository toolCallRepository;
    private final String model;
    private final Map<AgentScenario, String> systemPrompts;
    private final Map<AgentScenario, String> systemPromptVersions;

    public AgentRunner(
            AnthropicClient client,
            ScenarioToolProvider toolProvider,
            AgentRunRepository agentRunRepository,
            ToolCallRepository toolCallRepository,
            @Value("${anthropic.model}") String model,
            @Value("classpath:prompts/pantry-assistant/system-prompt.txt") Resource pantryAssistantPrompt) {
        this.client = client;
        this.toolProvider = toolProvider;
        this.agentRunRepository = agentRunRepository;
        this.toolCallRepository = toolCallRepository;
        this.model = model;

        this.systemPrompts = new EnumMap<>(AgentScenario.class);
        systemPrompts.put(AgentScenario.PANTRY_ASSISTANT, readResource(pantryAssistantPrompt));

        this.systemPromptVersions = systemPrompts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> sha256Hex(e.getValue()), (a, b) -> a, () -> new EnumMap<>(AgentScenario.class)));
    }

    public AgentRunOutcome run(Long userId, AgentScenario scenario, String trigger, String userMessage) {
        List<AgentTool> tools = toolProvider.toolsFor(scenario);
        Map<String, AgentTool> toolsByName = tools.stream().collect(Collectors.toMap(AgentTool::name, Function.identity()));

        AgentRun agentRun = agentRunRepository.save(
                new AgentRun(userId, scenario, trigger, systemPromptVersions.get(scenario)));

        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder().role(MessageParam.Role.USER).content(userMessage).build());

        int sequenceNumber = 0;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            agentRun.incrementIterationCount();

            Message response;
            try {
                MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                        .model(model)
                        .maxTokens(MAX_TOKENS)
                        .system(systemPrompts.get(scenario))
                        .messages(messages);
                tools.forEach(tool -> paramsBuilder.addTool(tool.definition()));
                response = client.messages().create(paramsBuilder.build());
            } catch (AnthropicException e) {
                return finishWith(agentRun, AgentRunStatus.LLM_TIMEOUT,
                        "The assistant is temporarily unavailable. Please try again shortly.");
            }

            List<ContentBlockParam> assistantContent = new ArrayList<>();
            List<ContentBlockParam> toolResults = new ArrayList<>();

            for (ContentBlock block : response.content()) {
                assistantContent.add(block.toParam());

                Optional<ToolUseBlock> toolUseOpt = block.toolUse();
                if (toolUseOpt.isEmpty()) {
                    continue;
                }
                ToolUseBlock toolUse = toolUseOpt.get();
                sequenceNumber++;

                AgentTool tool = toolsByName.get(toolUse.name());
                if (tool == null) {
                    // AI-14: the model asked for a tool this scenario never exposed.
                    logToolCall(agentRun, sequenceNumber, toolUse, null, true);
                    return finishWith(agentRun, AgentRunStatus.PERMISSION_DENIED,
                            "The assistant attempted an action it isn't permitted to take in this context.");
                }

                String resultText;
                boolean isError;
                try {
                    resultText = executeIdempotently(agentRun, sequenceNumber, tool, userId, toolUse._input());
                    isError = false;
                } catch (DataAccessException e) {
                    // AI-21b: infrastructure failure — never enters the reasoning loop, never retried.
                    logToolCall(agentRun, sequenceNumber, toolUse, e.getMessage(), true);
                    return finishWith(agentRun, AgentRunStatus.TOOL_ERROR_FATAL,
                            "Something went wrong on our end. Please try again later.");
                } catch (RuntimeException e) {
                    // AI-21a: recoverable domain error — goes back to the model as an observation.
                    resultText = e.getMessage();
                    isError = true;
                }

                logToolCall(agentRun, sequenceNumber, toolUse, resultText, isError);
                toolResults.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                        .toolUseId(toolUse.id())
                        .content(resultText)
                        .isError(isError)
                        .build()));
            }

            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.ASSISTANT)
                    .contentOfBlockParams(assistantContent)
                    .build());

            StopReason stopReason = response.stopReason().orElse(null);
            if (!StopReason.TOOL_USE.equals(stopReason)) {
                String finalText = response.content().stream()
                        .flatMap(block -> block.text().stream())
                        .map(TextBlock::text)
                        .collect(Collectors.joining("\n"));
                agentRun.finish(AgentRunStatus.FINAL_RESPONSE);
                agentRunRepository.save(agentRun);
                return AgentRunOutcome.success(finalText);
            }

            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(toolResults)
                    .build());
        }

        return finishWith(agentRun, AgentRunStatus.ITERATION_LIMIT,
                "I wasn't able to finish this within the allowed number of steps. "
                        + "Please try rephrasing or breaking your request into smaller pieces.");
    }

    /**
     * AI-15/AI-15a: keyed by (agent run, sequence number). A single call to {@link #run} never
     * revisits a sequence number itself, so this is a no-op today — but it's what makes a
     * future caller that resumes an in-progress run after a crash safe by construction,
     * without this method having to change.
     */
    private String executeIdempotently(AgentRun agentRun, int sequenceNumber, AgentTool tool, Long userId, JsonValue input) {
        if (tool.stateChanging()) {
            Optional<ToolCall> existing = toolCallRepository.findByAgentRunIdAndSequenceNumber(agentRun.getId(), sequenceNumber);
            if (existing.isPresent() && !existing.get().isError()) {
                return existing.get().getResult();
            }
        }
        return tool.execute(userId, input);
    }

    private void logToolCall(AgentRun agentRun, int sequenceNumber, ToolUseBlock toolUse, String result, boolean isError) {
        toolCallRepository.save(new ToolCall(
                agentRun, sequenceNumber, toolUse.name(), toolUse._input().toString(), result, isError));
    }

    private AgentRunOutcome finishWith(AgentRun agentRun, AgentRunStatus status, String userMessage) {
        agentRun.finish(status, userMessage);
        agentRunRepository.save(agentRun);
        return AgentRunOutcome.failure(status, userMessage);
    }

    private static String readResource(Resource resource) {
        try (var in = resource.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
