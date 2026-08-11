package de.mimosa_dev.MealPlanner.agent;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.DirectCaller;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.models.messages.Usage;
import com.anthropic.services.blocking.MessageService;
import de.mimosa_dev.MealPlanner.agent.tool.AgentTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRunnerTest {

    private static final Resource SYSTEM_PROMPT = new ClassPathResource("prompts/pantry-assistant/system-prompt.txt");
    private static final Long USER_ID = 1L;

    private AnthropicClient client;
    private MessageService messageService;
    private ScenarioToolProvider toolProvider;
    private AgentRunRepository agentRunRepository;
    private ToolCallRepository toolCallRepository;
    private AgentRunner runner;

    @BeforeEach
    void setUp() {
        client = mock(AnthropicClient.class);
        messageService = mock(MessageService.class);
        when(client.messages()).thenReturn(messageService);

        toolProvider = mock(ScenarioToolProvider.class);
        agentRunRepository = mock(AgentRunRepository.class);
        when(agentRunRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        toolCallRepository = mock(ToolCallRepository.class);
        when(toolCallRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolCallRepository.findByAgentRunIdAndSequenceNumber(any(), anyInt())).thenReturn(Optional.empty());

        runner = new AgentRunner(client, toolProvider, agentRunRepository, toolCallRepository,
                "claude-haiku-4-5", SYSTEM_PROMPT);
    }

    @Test
    void returnsTheFinalTextWhenTheModelNeedsNoTools() {
        when(toolProvider.toolsFor(AgentScenario.PANTRY_ASSISTANT)).thenReturn(List.of());
        when(messageService.create(any(MessageCreateParams.class))).thenReturn(textResponse("Hello there!"));

        AgentRunOutcome outcome = runner.run(USER_ID, AgentScenario.PANTRY_ASSISTANT, "user_message", "hi");

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.status()).isEqualTo(AgentRunStatus.FINAL_RESPONSE);
        assertThat(outcome.message()).isEqualTo("Hello there!");
    }

    @Test
    void executesAToolCallThenReturnsTheFollowingFinalResponse() {
        AgentTool fakeTool = fakeTool("get_pantry_contents", false, (userId, input) -> "Milk: 500 GRAM");
        when(toolProvider.toolsFor(AgentScenario.PANTRY_ASSISTANT)).thenReturn(List.of(fakeTool));
        when(messageService.create(any(MessageCreateParams.class)))
                .thenReturn(toolUseResponse("t1", "get_pantry_contents", Map.of()))
                .thenReturn(textResponse("You have milk."));

        AgentRunOutcome outcome = runner.run(USER_ID, AgentScenario.PANTRY_ASSISTANT, "user_message", "what do I have?");

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.message()).isEqualTo("You have milk.");
    }

    @Test
    void aRecoverableToolFailureIsReturnedToTheModelAsAnObservationInsteadOfAbortingTheRun() {
        AgentTool failingTool = fakeTool("consume_pantry_stock", true, (userId, input) -> {
            throw new IllegalStateException("not enough stock");
        });
        when(toolProvider.toolsFor(AgentScenario.PANTRY_ASSISTANT)).thenReturn(List.of(failingTool));
        when(messageService.create(any(MessageCreateParams.class)))
                .thenReturn(toolUseResponse("t1", "consume_pantry_stock", Map.of()))
                .thenReturn(textResponse("Looks like there isn't enough — want to add more first?"));

        AgentRunOutcome outcome = runner.run(USER_ID, AgentScenario.PANTRY_ASSISTANT, "user_message", "use up the milk");

        // the run still completes normally — the model got to react to the domain error
        assertThat(outcome.success()).isTrue();
        assertThat(outcome.status()).isEqualTo(AgentRunStatus.FINAL_RESPONSE);
    }

    @Test
    void anInfrastructureFailureAbortsTheRunWithoutRetrying() {
        AgentTool brokenTool = fakeTool("get_pantry_contents", false, (userId, input) -> {
            throw new DataRetrievalFailureException("connection lost");
        });
        when(toolProvider.toolsFor(AgentScenario.PANTRY_ASSISTANT)).thenReturn(List.of(brokenTool));
        when(messageService.create(any(MessageCreateParams.class)))
                .thenReturn(toolUseResponse("t1", "get_pantry_contents", Map.of()));

        AgentRunOutcome outcome = runner.run(USER_ID, AgentScenario.PANTRY_ASSISTANT, "user_message", "what do I have?");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.status()).isEqualTo(AgentRunStatus.TOOL_ERROR_FATAL);
    }

    @Test
    void anUnavailableModelEndsTheRunWithoutRetrying() {
        when(toolProvider.toolsFor(AgentScenario.PANTRY_ASSISTANT)).thenReturn(List.of());
        when(messageService.create(any(MessageCreateParams.class)))
                .thenThrow(new AnthropicException("network unreachable"));

        AgentRunOutcome outcome = runner.run(USER_ID, AgentScenario.PANTRY_ASSISTANT, "user_message", "hi");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.status()).isEqualTo(AgentRunStatus.LLM_TIMEOUT);
    }

    @Test
    void aModelThatNeverStopsCallingToolsHitsTheIterationLimit() {
        AgentTool loopingTool = fakeTool("get_pantry_contents", false, (userId, input) -> "still going");
        when(toolProvider.toolsFor(AgentScenario.PANTRY_ASSISTANT)).thenReturn(List.of(loopingTool));
        when(messageService.create(any(MessageCreateParams.class)))
                .thenReturn(toolUseResponse("t1", "get_pantry_contents", Map.of()));

        AgentRunOutcome outcome = runner.run(USER_ID, AgentScenario.PANTRY_ASSISTANT, "user_message", "keep checking");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.status()).isEqualTo(AgentRunStatus.ITERATION_LIMIT);
    }

    @Test
    void aToolTheScenarioNeverExposedIsRejectedRatherThanExecuted() {
        // the scenario has no tools at all, yet the mocked response asks for one anyway —
        // simulates defense-in-depth against a tool call that shouldn't be reachable in practice
        when(toolProvider.toolsFor(AgentScenario.PANTRY_ASSISTANT)).thenReturn(List.of());
        when(messageService.create(any(MessageCreateParams.class)))
                .thenReturn(toolUseResponse("t1", "delete_account", Map.of()));

        AgentRunOutcome outcome = runner.run(USER_ID, AgentScenario.PANTRY_ASSISTANT, "user_message", "hi");

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.status()).isEqualTo(AgentRunStatus.PERMISSION_DENIED);
    }

    private static AgentTool fakeTool(String name, boolean stateChanging, BiFunction<Long, JsonValue, String> executor) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Tool definition() {
                return Tool.builder().name(name).description("test tool")
                        .inputSchema(Tool.InputSchema.builder().build()).build();
            }

            @Override
            public boolean stateChanging() {
                return stateChanging;
            }

            @Override
            public String execute(Long userId, JsonValue input) {
                return executor.apply(userId, input);
            }
        };
    }

    private static Message textResponse(String text) {
        return baseMessage(StopReason.END_TURN)
                .content(List.of(ContentBlock.ofText(
                        TextBlock.builder().text(text).citations(Optional.empty()).build())))
                .build();
    }

    private static Message toolUseResponse(String toolUseId, String toolName, Map<String, Object> input) {
        ToolUseBlock toolUseBlock = ToolUseBlock.builder()
                .id(toolUseId)
                .name(toolName)
                .input(JsonValue.from(input))
                .caller(DirectCaller.builder().build())
                .build();
        return baseMessage(StopReason.TOOL_USE)
                .content(List.of(ContentBlock.ofToolUse(toolUseBlock)))
                .build();
    }

    // Message's instance methods are final (immutable SDK response type), so Mockito's inline
    // mock maker can't intercept them the way it can for interfaces like AnthropicClient below
    // — build a real one via the SDK's own builder instead of mocking it.
    private static Message.Builder baseMessage(StopReason stopReason) {
        return Message.builder()
                .id("msg_test")
                .model("claude-haiku-4-5")
                .stopReason(stopReason)
                .stopDetails(Optional.empty())
                .stopSequence(Optional.empty())
                .container(Optional.empty())
                .usage(Usage.builder()
                        .inputTokens(0L)
                        .outputTokens(0L)
                        .cacheCreation(Optional.empty())
                        .cacheCreationInputTokens(0L)
                        .cacheReadInputTokens(0L)
                        .serverToolUse(Optional.empty())
                        .serviceTier(Optional.empty())
                        .inferenceGeo(Optional.empty())
                        .build());
    }
}
