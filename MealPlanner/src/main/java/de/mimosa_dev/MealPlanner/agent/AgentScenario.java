package de.mimosa_dev.MealPlanner.agent;

/**
 * Determines which tools an agent run can see (AI-13) — never the model's own choice. New
 * scenarios arrive with recipe generation (PRD step 7) and shopping lists (step 10); extend
 * this enum then, don't widen an existing scenario to cover unrelated tools.
 */
public enum AgentScenario {
    PANTRY_ASSISTANT
}
