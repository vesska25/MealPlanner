package de.mimosa_dev.MealPlanner.agentspike;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The spike's one read-only tool (PRD 9.1 step 3): lets the model query the seeded product
 * catalogue (from step 2) by name. Description lives in an external resource file per AI-33.
 * Thrown away once tool-calling mechanics are verified — the real agent layer (step 6) scopes
 * its tool surface per scenario (AI-13), which this single always-available tool doesn't.
 */
@Component
class ProductLookupTool {

    static final String NAME = "lookup_products";

    private final ProductRepository productRepository;
    private final String description;

    ProductLookupTool(
            ProductRepository productRepository,
            @Value("classpath:prompts/lookup-products-tool-description.txt") Resource descriptionResource) {
        this.productRepository = productRepository;
        this.description = readResource(descriptionResource);
    }

    private static String readResource(Resource resource) {
        try (var in = resource.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Tool definition() {
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

    String execute(String query) {
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
