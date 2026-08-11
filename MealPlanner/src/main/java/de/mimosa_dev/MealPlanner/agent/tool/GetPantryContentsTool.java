package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GetPantryContentsTool implements AgentTool {

    public static final String NAME = "get_pantry_contents";

    private final PantryItemRepository pantryItemRepository;
    private final String description;

    public GetPantryContentsTool(
            PantryItemRepository pantryItemRepository,
            @Value("classpath:prompts/pantry-assistant/get-pantry-contents-tool.txt") Resource descriptionResource) {
        this.pantryItemRepository = pantryItemRepository;
        this.description = ToolDescriptions.read(descriptionResource);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean stateChanging() {
        return false;
    }

    @Override
    public Tool definition() {
        return Tool.builder()
                .name(NAME)
                .description(description)
                .inputSchema(Tool.InputSchema.builder().build())
                .build();
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        List<PantryItem> items = pantryItemRepository
                .findByUserIdAndStatusOrderByExpiresAtAsc(userId, PantryItemStatus.ACTIVE);
        if (items.isEmpty()) {
            return "The pantry is empty.";
        }
        return items.stream()
                .map(item -> "#%d %s: %s %s (expires %s)".formatted(
                        item.getId(), item.getProduct().getCanonicalName(),
                        item.getQuantity(), item.getUnit(), item.getExpiresAt()))
                .collect(Collectors.joining("\n"));
    }
}
