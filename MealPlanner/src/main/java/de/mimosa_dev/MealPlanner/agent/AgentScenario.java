package de.mimosa_dev.MealPlanner.agent;

/**
 * Determines which tools an agent run can see (AI-13) — never the model's own choice. A
 * shopping-list scenario arrives with step 10; extend this enum then, don't widen an existing
 * scenario to cover unrelated tools.
 */
public enum AgentScenario {
    PANTRY_ASSISTANT,
    MEAL_PLANNING
}
