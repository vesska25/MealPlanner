package de.mimosa_dev.MealPlanner.product;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReloadsProductWithSynonyms() {
        Product product = new Product();
        product.setCanonicalName("test product");
        product.setCategory(ProductCategory.OTHER);
        product.setDefaultShelfLifeDays(10);
        product.getSynonyms().add("test synonym");

        Long id = productRepository.saveAndFlush(product).getId();
        entityManager.clear();

        Product reloaded = productRepository.findById(id).orElseThrow();
        assertThat(reloaded.getCanonicalName()).isEqualTo("test product");
        assertThat(reloaded.getSynonyms()).containsExactly("test synonym");
        // JPA sends an explicit value on INSERT, so the column's DEFAULT TRUE (for the seed
        // migration's raw SQL) never applies here; a product built through the entity API
        // starts unverified unless the caller opts in (FR-43d).
        assertThat(reloaded.isVerified()).isFalse();
        assertThat(reloaded.isStaple()).isFalse();
    }

    @Test
    void rejectsDuplicateCanonicalName() {
        Product first = new Product();
        first.setCanonicalName("duplicate name");
        first.setCategory(ProductCategory.OTHER);
        first.setDefaultShelfLifeDays(10);
        productRepository.saveAndFlush(first);

        Product second = new Product();
        second.setCanonicalName("duplicate name");
        second.setCategory(ProductCategory.OTHER);
        second.setDefaultShelfLifeDays(5);

        assertThatThrownBy(() -> productRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seedMigrationLoadsStaplesAsSuch() {
        Product salt = productRepository.findAll().stream()
                .filter(p -> "salt".equals(p.getCanonicalName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("seed migration did not create 'salt'"));

        assertThat(salt.isStaple()).isTrue();
        assertThat(productRepository.findAll()).hasSizeGreaterThanOrEqualTo(30);
    }
}
