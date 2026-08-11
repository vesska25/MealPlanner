package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LookupProductsTool implements AgentTool {

    public static final String NAME = "lookup_products";

    private final ProductRepository productRepository;
    private final String description;

    public LookupProductsTool(
            ProductRepository productRepository,
            @Value("classpath:prompts/pantry-assistant/lookup-products-tool.txt") Resource descriptionResource) {
        this.productRepository = productRepository;
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
                .inputSchema(Tool.InputSchema.builder()
                        .properties(Tool.InputSchema.Properties.builder()
                                .putAdditionalProperty("query", JsonValue.from(Map.of(
                                        "type", "string",
                                        "description", "Substring to search for in the product's canonical name")))
                                .build())
                        .required(List.of("query"))
                        .build())
                .build();
    }

    private record Input(String query) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        String query = input.convert(Input.class).query();
        List<Product> matches = productRepository.findByCanonicalNameContainingIgnoreCase(query);
        if (matches.isEmpty()) {
            return "No products found matching \"" + query + "\".";
        }
        return matches.stream()
                .map(p -> "%s (%s, shelf life %dd)".formatted(
                        p.getCanonicalName(), p.getCategory(), p.getDefaultShelfLifeDays()))
                .collect(Collectors.joining("\n"));
    }
}
