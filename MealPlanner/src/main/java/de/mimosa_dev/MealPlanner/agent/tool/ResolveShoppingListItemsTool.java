package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListItemStatus;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * FR-44/FR-45/FR-45b: records what the user actually did after the shop — resolves specific
 * list items and/or adds free-text extras that weren't on the list at all.
 */
@Component
public class ResolveShoppingListItemsTool implements AgentTool {

    public static final String NAME = "resolve_shopping_list_items";

    private final ShoppingListService shoppingListService;
    private final String description;

    public ResolveShoppingListItemsTool(
            ShoppingListService shoppingListService,
            @Value("classpath:prompts/shopping-list/resolve-shopping-list-items-tool.txt") Resource descriptionResource) {
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
        Map<String, Object> itemResolutionSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "itemId", Map.of("type", "integer", "description", "The shopping list item's id, from generate_shopping_list"),
                        "status", Map.of("type", "string",
                                "enum", List.of("PURCHASED", "ALREADY_HAVE", "NOT_BUYING", "NOT_NEEDED"),
                                "description", "PURCHASED/ALREADY_HAVE add it to the pantry as an estimate; the others don't")),
                "required", List.of("itemId", "status"));

        Map<String, Object> extraPurchaseSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "productName", Map.of("type", "string", "description", "Canonical or free-text product name"),
                        "quantity", Map.of("type", "number", "description", "Amount bought, as a plain number"),
                        "unit", Map.of("type", "string", "enum", List.of("GRAM", "MILLILITER", "PIECE"))),
                "required", List.of("productName", "quantity", "unit"));

        return Tool.builder()
                .name(NAME)
                .description(description)
                .inputSchema(Tool.InputSchema.builder()
                        .properties(Tool.InputSchema.Properties.builder()
                                .putAdditionalProperty("items", JsonValue.from(Map.of(
                                        "type", "array", "items", itemResolutionSchema,
                                        "description", "Status updates for items that were on the list")))
                                .putAdditionalProperty("extraPurchases", JsonValue.from(Map.of(
                                        "type", "array", "items", extraPurchaseSchema,
                                        "description", "Things bought that weren't on the list at all")))
                                .build())
                        .build())
                .build();
    }

    private record ItemResolutionInput(Long itemId, ShoppingListItemStatus status) {
    }

    private record ExtraPurchaseInput(String productName, BigDecimal quantity, Unit unit) {
    }

    private record Input(List<ItemResolutionInput> items, List<ExtraPurchaseInput> extraPurchases) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        Input parsed = input.convert(Input.class);
        List<ItemResolutionInput> items = parsed.items() == null ? List.of() : parsed.items();
        List<ExtraPurchaseInput> extras = parsed.extraPurchases() == null ? List.of() : parsed.extraPurchases();

        shoppingListService.resolveItems(
                userId,
                items.stream().map(i -> new ShoppingListService.ItemResolution(i.itemId(), i.status())).toList(),
                extras.stream().map(e -> new ShoppingListService.ExtraPurchase(e.productName(), e.quantity(), e.unit())).toList());

        return "Recorded %d item update(s) and %d extra purchase(s). Pantry updated accordingly."
                .formatted(items.size(), extras.size());
    }
}
