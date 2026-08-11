package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ConsumePantryStockTool implements AgentTool {

    public static final String NAME = "consume_pantry_stock";

    private final ProductNormalizationService normalizationService;
    private final PantryService pantryService;
    private final String description;

    public ConsumePantryStockTool(
            ProductNormalizationService normalizationService,
            PantryService pantryService,
            @Value("classpath:prompts/pantry-assistant/consume-pantry-stock-tool.txt") Resource descriptionResource) {
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
                                        "description", "Amount used, as a plain number")))
                                .putAdditionalProperty("unit", JsonValue.from(Map.of(
                                        "type", "string",
                                        "enum", List.of("GRAM", "MILLILITER", "PIECE"),
                                        "description", "Canonical unit for the quantity")))
                                .build())
                        .required(List.of("productName", "quantity", "unit"))
                        .build())
                .build();
    }

    private record Input(String productName, BigDecimal quantity, Unit unit) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        Input parsed = input.convert(Input.class);
        Product product = normalizationService.resolve(parsed.productName());

        // InsufficientStockException propagates as-is — the runner treats it as a recoverable
        // domain error (AI-21a) and returns it to the model as an observation.
        pantryService.consume(userId, product.getId(), parsed.quantity(), parsed.unit());

        return "Consumed %s %s of %s.".formatted(parsed.quantity(), parsed.unit(), product.getCanonicalName());
    }
}
