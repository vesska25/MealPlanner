package de.mimosa_dev.MealPlanner.pantry;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PantryItemRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReloadsPantryItem() {
        Product salt = seededProduct("salt");
        PantryItem saved = pantryItemRepository.saveAndFlush(newItem(salt, new BigDecimal("500")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getStatus()).isEqualTo(PantryItemStatus.ACTIVE);
    }

    // INV-01 / NFR-43: the CHECK constraint is the last line of defense against a negative
    // quantity, independent of any service-level validation.
    @Test
    void rejectsNegativeQuantity() {
        Product salt = seededProduct("salt");
        PantryItem item = newItem(salt, new BigDecimal("-1"));

        assertThatThrownBy(() -> pantryItemRepository.saveAndFlush(item))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // NFR-43a: normal pantry operations rely on optimistic locking to catch a stale write
    // when two requests read the same row before either commits.
    @Test
    void staleWriteFailsOptimisticLock() {
        Product salt = seededProduct("salt");
        Long id = pantryItemRepository.saveAndFlush(newItem(salt, new BigDecimal("500"))).getId();
        entityManager.clear();

        PantryItem readByFirstRequest = pantryItemRepository.findById(id).orElseThrow();
        entityManager.clear();
        PantryItem readBySecondRequest = pantryItemRepository.findById(id).orElseThrow();
        entityManager.clear();

        readByFirstRequest.setQuantity(new BigDecimal("400"));
        pantryItemRepository.saveAndFlush(readByFirstRequest);

        readBySecondRequest.setQuantity(new BigDecimal("300"));
        assertThatThrownBy(() -> pantryItemRepository.saveAndFlush(readBySecondRequest))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private Product seededProduct(String canonicalName) {
        return productRepository.findAll().stream()
                .filter(p -> canonicalName.equals(p.getCanonicalName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("seed migration did not create '" + canonicalName + "'"));
    }

    private PantryItem newItem(Product product, BigDecimal quantity) {
        PantryItem item = new PantryItem();
        item.setUserId(1L);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnit(Unit.GRAM);
        item.setPurchasedAt(LocalDate.now());
        item.setExpiresAt(LocalDate.now().plusDays(30));
        item.setStatus(PantryItemStatus.ACTIVE);
        return item;
    }
}
