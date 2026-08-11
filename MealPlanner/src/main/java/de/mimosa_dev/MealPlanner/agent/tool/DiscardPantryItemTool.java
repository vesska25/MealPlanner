package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.pantry.DiscardReason;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DiscardPantryItemTool implements AgentTool {

    public static final String NAME = "discard_pantry_item";

    private final PantryService pantryService;
    private final String description;

    public DiscardPantryItemTool(
            PantryService pantryService,
            @Value("classpath:prompts/pantry-assistant/discard-pantry-item-tool.txt") Resource descriptionResource) {
        this.pantryService = pantryService;
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
                                .putAdditionalProperty("pantryItemId", JsonValue.from(Map.of(
                                        "type", "integer",
                                        "description", "The pantry item's id, from get_pantry_contents")))
                                .putAdditionalProperty("reason", JsonValue.from(Map.of(
                                        "type", "string",
                                        "enum", List.of("EXPIRED_EARLY", "DIDNT_COOK_IN_TIME", "BOUGHT_TOO_MUCH"),
                                        "description", "Why the item is being discarded")))
                                .build())
                        .required(List.of("pantryItemId", "reason"))
                        .build())
                .build();
    }

    private record Input(Long pantryItemId, DiscardReason reason) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        Input parsed = input.convert(Input.class);
        pantryService.discard(userId, parsed.pantryItemId(), parsed.reason());
        return "Discarded pantry item #%d (%s).".formatted(parsed.pantryItemId(), parsed.reason());
    }
}
