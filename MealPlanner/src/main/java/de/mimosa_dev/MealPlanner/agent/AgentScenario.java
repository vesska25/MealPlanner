package de.mimosa_dev.MealPlanner.agent;

/**
 * Determines which tools an agent run can see (AI-13) — never the model's own choice.
 */
public enum AgentScenario {
    PANTRY_ASSISTANT,
    MEAL_PLANNING,
    SHOPPING_LIST
}
