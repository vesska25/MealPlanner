package de.mimosa_dev.MealPlanner.product;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(ProductNormalizationService.class)
class ProductNormalizationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ProductNormalizationService normalizationService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void exactCanonicalNameMatchIsCaseInsensitive() {
        long before = productRepository.count();

        Product resolved = normalizationService.resolve("SALT");

        assertThat(resolved.getCanonicalName()).isEqualTo("salt");
        assertThat(productRepository.count()).isEqualTo(before);
    }

    @Test
    void synonymMatchReturnsTheOwningProduct() {
        Product tomato = productRepository.findByCanonicalNameIgnoreCase("tomato").orElseThrow();
        tomato.getSynonyms().add("pomodoro");
        productRepository.saveAndFlush(tomato);

        Product resolved = normalizationService.resolve("Pomodoro");

        assertThat(resolved.getId()).isEqualTo(tomato.getId());
    }

    @Test
    void closeMisspellingFuzzyMatchesTheExistingProduct() {
        // one missing letter in a long name keeps similarity comfortably above the threshold
        Product resolved = normalizationService.resolve("chedar cheese");

        assertThat(resolved.getCanonicalName()).isEqualTo("cheddar cheese");
    }

    @Test
    void unmatchedNameCreatesAnUnverifiedProduct() {
        long before = productRepository.count();

        Product resolved = normalizationService.resolve("dragon fruit smoothie mix");

        assertThat(resolved.getId()).isNotNull();
        assertThat(resolved.getCanonicalName()).isEqualTo("dragon fruit smoothie mix");
        assertThat(resolved.isVerified()).isFalse();
        assertThat(resolved.getCategory()).isEqualTo(ProductCategory.OTHER);
        assertThat(productRepository.count()).isEqualTo(before + 1);
    }
}
