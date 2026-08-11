package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.recipe.RejectionReason;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * FR-60/FR-61: rejects the currently active suggestion with a reason. Scoped to the
 * MEAL_PLANNING scenario only — see {@code ScenarioToolProvider}.
 */
@Component
public class RejectSuggestionTool implements AgentTool {

    public static final String NAME = "reject_suggestion";

    private final RecipeSuggestionService recipeSuggestionService;
    private final String description;

    public RejectSuggestionTool(
            RecipeSuggestionService recipeSuggestionService,
            @Value("classpath:prompts/meal-planning/reject-suggestion-tool.txt") Resource descriptionResource) {
        this.recipeSuggestionService = recipeSuggestionService;
        this.description = ToolDescriptions.read(descriptionResource);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean stateChanging() {
        return true;
    }

    @Override
    public Tool definition() {
        return Tool.builder()
                .name(NAME)
                .description(description)
                .inputSchema(Tool.InputSchema.builder()
                        .properties(Tool.InputSchema.Properties.builder()
                                .putAdditionalProperty("recipeId", JsonValue.from(Map.of(
                                        "type", "integer",
                                        "description", "The id of the currently suggested recipe, from propose_recipe_candidates")))
                                .putAdditionalProperty("reason", JsonValue.from(Map.of(
                                        "type", "string",
                                        "enum", List.of("DISLIKE_DISH", "NOT_TODAY", "TAKES_TOO_LONG",
                                                "DONT_WANT_CATEGORY", "TIRED_OF_INGREDIENT"),
                                        "description", "Why the user is rejecting this suggestion")))
                                .build())
                        .required(List.of("recipeId", "reason"))
                        .build())
                .build();
    }

    private record Input(Long recipeId, RejectionReason reason) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        Input parsed = input.convert(Input.class);
        recipeSuggestionService.reject(userId, parsed.recipeId(), parsed.reason());
        return "Rejected. Propose 2-3 fresh alternatives now that address the stated reason.";
    }
}
