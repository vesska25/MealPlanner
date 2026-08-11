package de.mimosa_dev.MealPlanner.agent.dto;

import de.mimosa_dev.MealPlanner.agent.AgentRunStatus;

/**
 * Mirrors {@link de.mimosa_dev.MealPlanner.agent.AgentRunOutcome} directly. {@code status} is
 * exposed deliberately, not just {@code message} — the frontend needs it to render
 * {@code FALLBACK_RESPONSE} distinctly rather than presenting it as a full pick (AI-20c).
 */
public record ChatResponse(boolean success, AgentRunStatus status, String message) {
}
