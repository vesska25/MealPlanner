package de.mimosa_dev.MealPlanner.agent.controller;

import de.mimosa_dev.MealPlanner.agent.AgentRunOutcome;
import de.mimosa_dev.MealPlanner.agent.AgentRunner;
import de.mimosa_dev.MealPlanner.agent.AgentScenario;
import de.mimosa_dev.MealPlanner.agent.dto.ChatRequest;
import de.mimosa_dev.MealPlanner.agent.dto.ChatResponse;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * The web client's entry point into the three chat-driven scenarios (PRD 9.1 step 11). Each
 * call is a self-contained agent run — {@link AgentRunner#run} builds a fresh message list per
 * call, so there's no cross-request conversation memory here to preserve or break.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    private static final String TRIGGER = "web";

    private final AgentRunner agentRunner;
    private final OnboardingDraftService onboardingDraftService;

    public AgentChatController(AgentRunner agentRunner, OnboardingDraftService onboardingDraftService) {
        this.agentRunner = agentRunner;
        this.onboardingDraftService = onboardingDraftService;
    }

    @PostMapping("/{scenario}/messages")
    public ResponseEntity<ChatResponse> sendMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable String scenario,
            @Valid @RequestBody ChatRequest request) {
        AgentScenario parsedScenario;
        try {
            parsedScenario = AgentScenario.valueOf(scenario.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        // FR-10a: unlike every other scenario (stateless per call, see AgentRunner's own
        // javadoc), onboarding needs the backend to compose the draft-so-far and recent turns
        // into the message itself, and to persist both sides of this turn afterward — entirely
        // outside AgentRunner.run, which needs no change for this.
        boolean isOnboarding = parsedScenario == AgentScenario.ONBOARDING;
        String effectiveMessage = isOnboarding
                ? onboardingDraftService.buildContextualMessage(userId, request.message())
                : request.message();

        AgentRunOutcome outcome = agentRunner.run(userId, parsedScenario, TRIGGER, effectiveMessage);

        if (isOnboarding) {
            onboardingDraftService.appendTurn(userId, "user", request.message());
            onboardingDraftService.appendTurn(userId, "agent", outcome.message());
        }

        return ResponseEntity.ok(new ChatResponse(outcome.success(), outcome.status(), outcome.message()));
    }
}
