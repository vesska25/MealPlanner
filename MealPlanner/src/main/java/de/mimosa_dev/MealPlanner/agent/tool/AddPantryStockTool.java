package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class AddPantryStockTool implements AgentTool {

    public static final String NAME = "add_pantry_stock";

    private final ProductNormalizationService normalizationService;
    private final PantryService pantryService;
    private final String description;

    public AddPantryStockTool(
            ProductNormalizationService normalizationService,
            PantryService pantryService,
            @Value("classpath:prompts/pantry-assistant/add-pantry-stock-tool.txt") Resource descriptionResource) {
        this.normalizationService = normalizationService;
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
                                .putAdditionalProperty("productName", JsonValue.from(Map.of(
                                        "type", "string",
                                        "description", "Canonical or free-text product name, e.g. \"milk\"")))
                                .putAdditionalProperty("quantity", JsonValue.from(Map.of(
                                        "type", "number",
                                        "description", "Amount bought, as a plain number")))
                                .putAdditionalProperty("unit", JsonValue.from(Map.of(
                                        "type", "string",
                                        "enum", List.of("GRAM", "MILLILITER", "PIECE"),
                                        "description", "Canonical unit for the quantity")))
                                .putAdditionalProperty("purchasedAt", JsonValue.from(Map.of(
                                        "type", "string",
                                        "description", "ISO date (YYYY-MM-DD) the item was bought; omit for today")))
                                .build())
                        .required(List.of("productName", "quantity", "unit"))
                        .build())
                .build();
    }

    private record Input(String productName, BigDecimal quantity, Unit unit, String purchasedAt) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        Input parsed = input.convert(Input.class);
        Product product = normalizationService.resolve(parsed.productName());
        LocalDate purchasedAt = parsed.purchasedAt() == null || parsed.purchasedAt().isBlank()
                ? LocalDate.now()
                : LocalDate.parse(parsed.purchasedAt());

        PantryItem saved = pantryService.addStock(userId, product, parsed.quantity(), parsed.unit(), purchasedAt);

        return "Added %s %s of %s to the pantry (item #%d, expires %s).".formatted(
                saved.getQuantity(), saved.getUnit(), product.getCanonicalName(),
                saved.getId(), saved.getExpiresAt());
    }
}
