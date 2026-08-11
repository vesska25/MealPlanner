package de.mimosa_dev.MealPlanner.pantry;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(PantryService.class)
class PantryServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void addStockDerivesExpiryFromProductShelfLife() {
        Product salt = seededProduct("salt"); // default_shelf_life_days = 3650

        PantryItem saved = pantryService.addStock(
                USER_ID, salt, new BigDecimal("500"), Unit.GRAM, LocalDate.of(2026, 1, 1));

        assertThat(saved.getExpiresAt()).isEqualTo(LocalDate.of(2026, 1, 1).plusDays(3650));
        assertThat(saved.getStatus()).isEqualTo(PantryItemStatus.ACTIVE);
    }

    @Test
    void consumePartiallyDrainsASinglePosition() {
        Product milk = seededProduct("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("1000"), Unit.GRAM, LocalDate.now());
        entityManager.clear();

        pantryService.consume(USER_ID, milk.getId(), new BigDecimal("400"), Unit.GRAM);
        entityManager.flush();
        entityManager.clear();

        PantryItem remaining = onlyActiveItem(milk);
        assertThat(remaining.getQuantity()).isEqualByComparingTo("600");
        assertThat(remaining.getStatus()).isEqualTo(PantryItemStatus.ACTIVE);
    }

    @Test
    void consumingExactlyWhatsLeftMarksThePositionConsumed() {
        Product milk = seededProduct("milk");
        PantryItem item = pantryService.addStock(
                USER_ID, milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());
        entityManager.clear();

        pantryService.consume(USER_ID, milk.getId(), new BigDecimal("500"), Unit.GRAM);
        entityManager.flush();
        entityManager.clear();

        PantryItem consumed = pantryItemRepository.findById(item.getId()).orElseThrow();
        assertThat(consumed.getQuantity()).isEqualByComparingTo("0");
        assertThat(consumed.getStatus()).isEqualTo(PantryItemStatus.CONSUMED);
    }

    @Test
    void consumeDrainsOldestExpiryFirst() {
        Product milk = seededProduct("milk");
        PantryItem soonToExpire = pantryService.addStock(
                USER_ID, milk, new BigDecimal("300"), Unit.GRAM, LocalDate.now().minusDays(5));
        PantryItem freshest = pantryService.addStock(
                USER_ID, milk, new BigDecimal("300"), Unit.GRAM, LocalDate.now());
        entityManager.clear();
        assertThat(soonToExpire.getExpiresAt()).isBefore(freshest.getExpiresAt());

        // needs more than the first (oldest) position alone provides
        pantryService.consume(USER_ID, milk.getId(), new BigDecimal("400"), Unit.GRAM);
        entityManager.flush();
        entityManager.clear();

        PantryItem oldest = pantryItemRepository.findById(soonToExpire.getId()).orElseThrow();
        PantryItem newest = pantryItemRepository.findById(freshest.getId()).orElseThrow();
        assertThat(oldest.getQuantity()).isEqualByComparingTo("0");
        assertThat(oldest.getStatus()).isEqualTo(PantryItemStatus.CONSUMED);
        assertThat(newest.getQuantity()).isEqualByComparingTo("200");
        assertThat(newest.getStatus()).isEqualTo(PantryItemStatus.ACTIVE);
    }

    @Test
    void consumingMoreThanAvailableRejectsTheWholeOperation() {
        Product milk = seededProduct("milk");
        pantryService.addStock(USER_ID, milk, new BigDecimal("100"), Unit.GRAM, LocalDate.now());
        entityManager.clear();

        assertThatThrownBy(() -> pantryService.consume(USER_ID, milk.getId(), new BigDecimal("150"), Unit.GRAM))
                .isInstanceOf(InsufficientStockException.class);

        entityManager.clear();
        PantryItem untouched = onlyActiveItem(milk);
        assertThat(untouched.getQuantity()).isEqualByComparingTo("100");
    }

    @Test
    void discardMarksAnActiveItemDiscardedWithReason() {
        Product milk = seededProduct("milk");
        PantryItem item = pantryService.addStock(
                USER_ID, milk, new BigDecimal("200"), Unit.GRAM, LocalDate.now());
        entityManager.clear();

        pantryService.discard(USER_ID, item.getId(), DiscardReason.EXPIRED_EARLY);
        entityManager.flush();
        entityManager.clear();

        PantryItem discarded = pantryItemRepository.findById(item.getId()).orElseThrow();
        assertThat(discarded.getStatus()).isEqualTo(PantryItemStatus.DISCARDED);
        assertThat(discarded.getDiscardReason()).isEqualTo(DiscardReason.EXPIRED_EARLY);
    }

    @Test
    void discardingAnAlreadyDiscardedItemFails() {
        Product milk = seededProduct("milk");
        PantryItem item = pantryService.addStock(
                USER_ID, milk, new BigDecimal("200"), Unit.GRAM, LocalDate.now());
        pantryService.discard(USER_ID, item.getId(), DiscardReason.BOUGHT_TOO_MUCH);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> pantryService.discard(USER_ID, item.getId(), DiscardReason.DIDNT_COOK_IN_TIME))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void discardingAnotherUsersItemIsTreatedAsNotFound() {
        Product milk = seededProduct("milk");
        PantryItem item = pantryService.addStock(
                USER_ID, milk, new BigDecimal("200"), Unit.GRAM, LocalDate.now());
        entityManager.flush();
        entityManager.clear();

        Long someoneElse = USER_ID + 1;
        assertThatThrownBy(() -> pantryService.discard(someoneElse, item.getId(), DiscardReason.BOUGHT_TOO_MUCH))
                .isInstanceOf(NoSuchElementException.class);
    }

    private Product seededProduct(String canonicalName) {
        return productRepository.findByCanonicalNameIgnoreCase(canonicalName).orElseThrow();
    }

    private PantryItem onlyActiveItem(Product product) {
        List<PantryItem> active = pantryItemRepository
                .findByUserIdAndProductIdAndUnitAndStatusOrderByExpiresAtAsc(
                        USER_ID, product.getId(), Unit.GRAM, PantryItemStatus.ACTIVE);
        assertThat(active).hasSize(1);
        return active.get(0);
    }
}
