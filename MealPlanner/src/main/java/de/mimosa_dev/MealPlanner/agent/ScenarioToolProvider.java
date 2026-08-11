package de.mimosa_dev.MealPlanner.agent;

import de.mimosa_dev.MealPlanner.agent.tool.AddPantryStockTool;
import de.mimosa_dev.MealPlanner.agent.tool.AgentTool;
import de.mimosa_dev.MealPlanner.agent.tool.ConsumePantryStockTool;
import de.mimosa_dev.MealPlanner.agent.tool.DiscardPantryItemTool;
import de.mimosa_dev.MealPlanner.agent.tool.GetPantryContentsTool;
import de.mimosa_dev.MealPlanner.agent.tool.LookupProductsTool;
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
            DiscardPantryItemTool discardPantryItemTool) {
        this.toolsByScenario = new EnumMap<>(AgentScenario.class);
        toolsByScenario.put(AgentScenario.PANTRY_ASSISTANT, List.of(
                lookupProductsTool, getPantryContentsTool, addPantryStockTool,
                consumePantryStockTool, discardPantryItemTool));
    }

    public List<AgentTool> toolsFor(AgentScenario scenario) {
        List<AgentTool> tools = toolsByScenario.get(scenario);
        if (tools == null) {
            throw new IllegalArgumentException("No tools configured for scenario " + scenario);
        }
        return tools;
    }
}
