package de.mimosa_dev.MealPlanner.agent;

import de.mimosa_dev.MealPlanner.agent.tool.AddPantryStockTool;
import de.mimosa_dev.MealPlanner.agent.tool.AgentTool;
import de.mimosa_dev.MealPlanner.agent.tool.ConsumePantryStockTool;
import de.mimosa_dev.MealPlanner.agent.tool.DiscardPantryItemTool;
import de.mimosa_dev.MealPlanner.agent.tool.FinalizeOnboardingTool;
import de.mimosa_dev.MealPlanner.agent.tool.GenerateShoppingListTool;
import de.mimosa_dev.MealPlanner.agent.tool.GetPantryContentsTool;
import de.mimosa_dev.MealPlanner.agent.tool.LookupProductsTool;
import de.mimosa_dev.MealPlanner.agent.tool.ProposeRecipeCandidatesTool;
import de.mimosa_dev.MealPlanner.agent.tool.RejectSuggestionTool;
import de.mimosa_dev.MealPlanner.agent.tool.ResolveShoppingListItemsTool;
import de.mimosa_dev.MealPlanner.agent.tool.UpdateOnboardingDraftTool;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The single source of truth for AI-13: which tools a scenario exposes. {@link AgentRunner}
 * never receives more tools than this returns for the scenario it's running — the model is
 * structurally unable to call a tool from a different scenario, because it was never sent one.
 */
@Component
public class ScenarioToolProvider {

    private final Map<AgentScenario, List<AgentTool>> toolsByScenario;

    public ScenarioToolProvider(
            LookupProductsTool lookupProductsTool,
            GetPantryContentsTool getPantryContentsTool,
            AddPantryStockTool addPantryStockTool,
            ConsumePantryStockTool consumePantryStockTool,
            DiscardPantryItemTool discardPantryItemTool,
            ProposeRecipeCandidatesTool proposeRecipeCandidatesTool,
            RejectSuggestionTool rejectSuggestionTool,
            GenerateShoppingListTool generateShoppingListTool,
            ResolveShoppingListItemsTool resolveShoppingListItemsTool,
            UpdateOnboardingDraftTool updateOnboardingDraftTool,
            FinalizeOnboardingTool finalizeOnboardingTool) {
        this.toolsByScenario = new EnumMap<>(AgentScenario.class);
        toolsByScenario.put(AgentScenario.PANTRY_ASSISTANT, List.of(
                lookupProductsTool, getPantryContentsTool, addPantryStockTool,
                consumePantryStockTool, discardPantryItemTool));
        // Meal planning only reads pantry context and proposes recipes — it doesn't mutate
        // stock directly (AI-13: tool set is scoped to what the scenario actually needs).
        toolsByScenario.put(AgentScenario.MEAL_PLANNING, List.of(
                lookupProductsTool, getPantryContentsTool, proposeRecipeCandidatesTool, rejectSuggestionTool));
        // Shopping list mutates pantry only through resolve_shopping_list_items' own internal
        // PantryService calls, never by exposing add/consume_pantry_stock directly.
        toolsByScenario.put(AgentScenario.SHOPPING_LIST, List.of(
                lookupProductsTool, getPantryContentsTool, generateShoppingListTool, resolveShoppingListItemsTool));
        // Onboarding has no pantry/recipe/shopping tools at all (AI-13's own worked example:
        // "the onboarding scenario has no access to stock consumption").
        toolsByScenario.put(AgentScenario.ONBOARDING, List.of(updateOnboardingDraftTool, finalizeOnboardingTool));
    }

    public List<AgentTool> toolsFor(AgentScenario scenario) {
        List<AgentTool> tools = toolsByScenario.get(scenario);
        if (tools == null) {
            throw new IllegalArgumentException("No tools configured for scenario " + scenario);
        }
        return tools;
    }
}
