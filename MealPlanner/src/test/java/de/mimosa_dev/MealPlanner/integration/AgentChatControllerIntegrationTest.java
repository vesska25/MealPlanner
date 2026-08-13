package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.agent.AgentRunOutcome;
import de.mimosa_dev.MealPlanner.agent.AgentRunStatus;
import de.mimosa_dev.MealPlanner.agent.AgentRunner;
import de.mimosa_dev.MealPlanner.agent.AgentScenario;
import de.mimosa_dev.MealPlanner.agent.dto.ChatRequest;
import de.mimosa_dev.MealPlanner.agent.dto.ChatResponse;
import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Doesn't exercise a real agent run (that's already covered by {@code AgentRunnerTest}'s mocked-
 * {@code AnthropicClient} unit tests) — {@link AgentRunner} is replaced with a Mockito mock here
 * so this only tests the controller's own plumbing (auth extraction, path validation, response
 * mapping) without spending real API calls on every test run.
 */
class AgentChatControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OnboardingDraftService onboardingDraftService;

    @MockitoBean
    private AgentRunner agentRunner;

    @Test
    void aValidScenarioReturnsTheAgentsOutcome() {
        when(agentRunner.run(any(), eq(AgentScenario.PANTRY_ASSISTANT), eq("web"), any()))
                .thenReturn(AgentRunOutcome.success("You have milk."));
        String token = register();

        ResponseEntity<ChatResponse> response = exchangeAuthenticated(
                token, "/api/agent/pantry_assistant/messages", new ChatRequest("what do I have?"), ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().status()).isEqualTo(AgentRunStatus.FINAL_RESPONSE);
        assertThat(response.getBody().message()).isEqualTo("You have milk.");
    }

    @Test
    void anUnknownScenarioIsRejectedAsABadRequest() {
        String token = register();

        ResponseEntity<String> response = exchangeAuthenticated(
                token, "/api/agent/not-a-real-scenario/messages", new ChatRequest("hi"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/agent/pantry_assistant/messages", HttpMethod.POST,
                new HttpEntity<>(new ChatRequest("hi")), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // FR-10a: unlike every other scenario, onboarding needs the backend to compose draft-plus-
    // recent-turns context into the message it sends, and to persist both sides of the turn
    // afterward — this is the one piece of AgentChatController's own logic (not AgentRunner's)
    // worth a dedicated test.
    @Test
    void onboardingScenarioSendsDraftContextAndPersistsBothSidesOfTheTurn() {
        when(agentRunner.run(any(), eq(AgentScenario.ONBOARDING), eq("web"), any()))
                .thenReturn(AgentRunOutcome.success("How many people do you cook for?"));
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String token = register(email);
        Long userId = appUserRepository.findByEmail(email).orElseThrow().getId();

        ResponseEntity<ChatResponse> response = exchangeAuthenticated(
                token, "/api/agent/onboarding/messages", new ChatRequest("hi, let's start"), ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(agentRunner).run(eq(userId), eq(AgentScenario.ONBOARDING), eq("web"), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("Current draft profile").contains("User: hi, let's start");

        assertThat(onboardingDraftService.recentTurns(userId)).extracting(turn -> turn.text())
                .containsExactly("hi, let's start", "How many people do you cook for?");
    }

    private String register() {
        return register("user-" + UUID.randomUUID() + "@example.com");
    }

    private String register(String email) {
        String code = "TEST-" + UUID.randomUUID();
        inviteCodeRepository.save(new InviteCode(code));
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "password123", code), AuthResponse.class);
        return response.getBody().token();
    }

    private <T> ResponseEntity<T> exchangeAuthenticated(String token, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }
}
