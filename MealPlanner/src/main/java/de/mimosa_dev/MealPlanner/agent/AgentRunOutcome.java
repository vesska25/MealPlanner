package de.mimosa_dev.MealPlanner.agent;

/** What {@link AgentRunner#run} hands back — always a result, never a thrown exception. */
public record AgentRunOutcome(boolean success, AgentRunStatus status, String message) {

    public static AgentRunOutcome success(String message) {
        return new AgentRunOutcome(true, AgentRunStatus.FINAL_RESPONSE, message);
    }

    public static AgentRunOutcome failure(AgentRunStatus status, String message) {
        return new AgentRunOutcome(false, status, message);
    }

    /** AI-20a: the deterministic fallback succeeded — a real, usable result, just not a full pick. */
    public static AgentRunOutcome fallback(String message) {
        return new AgentRunOutcome(true, AgentRunStatus.FALLBACK_RESPONSE, message);
    }
}
