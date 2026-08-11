package de.mimosa_dev.MealPlanner.agent.controller;

import de.mimosa_dev.MealPlanner.agent.AgentRunOutcome;
import de.mimosa_dev.MealPlanner.agent.AgentRunner;
import de.mimosa_dev.MealPlanner.agent.AgentScenario;
import de.mimosa_dev.MealPlanner.agent.dto.ChatRequest;
import de.mimosa_dev.MealPlanner.agent.dto.ChatResponse;
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

    public AgentChatController(AgentRunner agentRunner) {
        this.agentRunner = agentRunner;
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

        AgentRunOutcome outcome = agentRunner.run(userId, parsedScenario, TRIGGER, request.message());
        return ResponseEntity.ok(new ChatResponse(outcome.success(), outcome.status(), outcome.message()));
    }
}
