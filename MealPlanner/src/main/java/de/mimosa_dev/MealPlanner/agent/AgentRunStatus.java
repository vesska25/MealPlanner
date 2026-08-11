package de.mimosa_dev.MealPlanner.agent;

/**
 * Terminal (and one in-progress) states of an agent run — mirrors PRD section 4.6's state
 * machine exactly. {@code TOOL_ERROR_RECOVERABLE} from that table isn't here: it loops back
 * into {@code THINK} rather than ending the run, so it's never a final status.
 */
public enum AgentRunStatus {
    RUNNING,
    FINAL_RESPONSE,
    TOOL_ERROR_FATAL,
    PERMISSION_DENIED,
    VALIDATION_FAILED,
    ITERATION_LIMIT,
    LLM_TIMEOUT,
    BUDGET_EXCEEDED,
    // AI-20a: the meal-planning scenario's deterministic fallback, taken after ITERATION_LIMIT
    // would otherwise fire. A successful outcome (the user gets a usable pick), but explicitly
    // distinguished so the UI can mark it as a fallback rather than a full pick (AI-20c).
    FALLBACK_RESPONSE
}
