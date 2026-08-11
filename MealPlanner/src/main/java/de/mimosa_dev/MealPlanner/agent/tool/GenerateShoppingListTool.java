package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListBlock;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListItem;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FR-40/FR-42: needs no model input at all — both blocks are fully Java-computed from the
 * active suggestion and current pantry state (PRD step 10, Phase B).
 */
@Component
public class GenerateShoppingListTool implements AgentTool {

    public static final String NAME = "generate_shopping_list";

    private final ShoppingListService shoppingListService;
    private final String description;

    public GenerateShoppingListTool(
            ShoppingListService shoppingListService,
            @Value("classpath:prompts/shopping-list/generate-shopping-list-tool.txt") Resource descriptionResource) {
        this.shoppingListService = shoppingListService;
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
                .inputSchema(Tool.InputSchema.builder().build())
                .build();
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        ShoppingListService.GeneratedShoppingList result = shoppingListService.generate(userId);
        List<ShoppingListItem> items = result.shoppingList().getItems();

        if (items.isEmpty()) {
            return "Nothing to buy right now — no active suggestion to shop for."
                    + (result.pantryRunningLow() ? " Pantry is running low, though — might be worth a general restock." : "");
        }

        StringBuilder text = new StringBuilder();
        if (result.pantryRunningLow()) {
            text.append("Pantry looks like it's running low.\n\n");
        }

        appendBlock(text, "Definitely need:", items, ShoppingListBlock.DEFINITELY_NEED);
        appendBlock(text, "Check — might already have:", items, ShoppingListBlock.CHECK_MAYBE_OUT);

        return text.toString().strip();
    }

    private static void appendBlock(
            StringBuilder text, String header, List<ShoppingListItem> items, ShoppingListBlock block) {
        List<ShoppingListItem> inBlock = items.stream().filter(item -> item.getBlock() == block).toList();
        if (inBlock.isEmpty()) {
            return;
        }
        text.append(header).append('\n');
        text.append(inBlock.stream()
                .map(item -> "#%d %s ~%s %s".formatted(
                        item.getId(), item.getProduct().getCanonicalName(), item.getQuantity(), item.getUnit()))
                .collect(Collectors.joining("\n")));
        text.append("\n\n");
    }
}
