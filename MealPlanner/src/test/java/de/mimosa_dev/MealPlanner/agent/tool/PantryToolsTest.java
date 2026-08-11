package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.InsufficientStockException;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        LookupProductsTool.class, GetPantryContentsTool.class, AddPantryStockTool.class,
        ConsumePantryStockTool.class, DiscardPantryItemTool.class,
        ProductNormalizationService.class, PantryService.class
})
class PantryToolsTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private LookupProductsTool lookupProductsTool;
    @Autowired
    private GetPantryContentsTool getPantryContentsTool;
    @Autowired
    private AddPantryStockTool addPantryStockTool;
    @Autowired
    private ConsumePantryStockTool consumePantryStockTool;
    @Autowired
    private DiscardPantryItemTool discardPantryItemTool;
    @Autowired
    private ProductNormalizationService normalizationService;
    @Autowired
    private PantryService pantryService;
    @Autowired
    private EntityManager entityManager;

    @Test
    void lookupProductsFindsASeededProduct() {
        String result = lookupProductsTool.execute(USER_ID, JsonValue.from(Map.of("query", "milk")));

        assertThat(result).contains("milk");
    }

    @Test
    void lookupProductsReportsNoMatch() {
        String result = lookupProductsTool.execute(USER_ID, JsonValue.from(Map.of("query", "zzz-nonexistent")));

        assertThat(result).contains("No products found");
    }

    @Test
    void getPantryContentsReportsAnEmptyPantry() {
        String result = getPantryContentsTool.execute(USER_ID, JsonValue.from(Map.of()));

        assertThat(result).isEqualTo("The pantry is empty.");
    }

    @Test
    void addPantryStockThenGetPantryContentsShowsTheAddedItem() {
        String addResult = addPantryStockTool.execute(USER_ID, JsonValue.from(Map.of(
                "productName", "milk", "quantity", 500, "unit", "GRAM")));
        assertThat(addResult).contains("milk");
        entityManager.clear();

        String contents = getPantryContentsTool.execute(USER_ID, JsonValue.from(Map.of()));

        assertThat(contents).contains("milk").contains("500");
    }

    @Test
    void addPantryStockResolvesAnUnknownNameToANewUnverifiedProduct() {
        String result = addPantryStockTool.execute(USER_ID, JsonValue.from(Map.of(
                "productName", "dragon fruit", "quantity", 2, "unit", "PIECE")));

        assertThat(result).contains("dragon fruit");
    }

    @Test
    void consumePantryStockReducesStock() {
        addPantryStockTool.execute(USER_ID, JsonValue.from(Map.of(
                "productName", "milk", "quantity", 500, "unit", "GRAM")));
        entityManager.flush();
        entityManager.clear();

        String result = consumePantryStockTool.execute(USER_ID, JsonValue.from(Map.of(
                "productName", "milk", "quantity", 200, "unit", "GRAM")));

        assertThat(result).contains("Consumed").contains("milk");
    }

    @Test
    void consumingMoreThanAvailableThrowsARecoverableDomainException() {
        addPantryStockTool.execute(USER_ID, JsonValue.from(Map.of(
                "productName", "milk", "quantity", 100, "unit", "GRAM")));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> consumePantryStockTool.execute(USER_ID, JsonValue.from(Map.of(
                "productName", "milk", "quantity", 500, "unit", "GRAM"))))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void discardPantryItemMarksItDiscarded() {
        Product milk = normalizationService.resolve("milk");
        PantryItem item = pantryService.addStock(USER_ID, milk, new BigDecimal("300"), Unit.GRAM, LocalDate.now());
        entityManager.flush();
        entityManager.clear();

        String result = discardPantryItemTool.execute(USER_ID, JsonValue.from(Map.of(
                "pantryItemId", item.getId(), "reason", "BOUGHT_TOO_MUCH")));

        assertThat(result).contains("Discarded").contains(item.getId().toString());
    }

    @Test
    void toolDefinitionsAreWellFormed() {
        for (AgentTool tool : List.of(lookupProductsTool, getPantryContentsTool, addPantryStockTool,
                consumePantryStockTool, discardPantryItemTool)) {
            assertThat(tool.definition().name()).isEqualTo(tool.name());
            assertThat(tool.definition().description()).isPresent();
        }
    }
}
