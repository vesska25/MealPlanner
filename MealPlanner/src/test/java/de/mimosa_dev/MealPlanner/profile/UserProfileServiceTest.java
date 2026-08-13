package de.mimosa_dev.MealPlanner.profile;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.recipe.UserConstraints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(UserProfileService.class)
class UserProfileServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void constraintsForFallsBackToDefaultsWhenOnboardingIsIncomplete() {
        // INV-04 note: nothing known is being bypassed here — a user with no profile row yet
        // has no real exclusions to violate. See UserProfileService's own javadoc.
        UserConstraints constraints = userProfileService.constraintsFor(USER_ID);

        assertThat(constraints).isEqualTo(UserConstraints.defaults());
    }

    @Test
    void constraintsForReflectsTheRealProfileOnceOnboardingIsComplete() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        UserProfile profile = new UserProfile(USER_ID, 3, 30);
        profile.getExcludedProductIds().add(milk.getId());
        profile.getEquipment().add("blender");
        userProfileRepository.saveAndFlush(profile);

        UserConstraints constraints = userProfileService.constraintsFor(USER_ID);

        assertThat(constraints.excludedProductIds()).containsExactly(milk.getId());
        assertThat(constraints.availableEquipment()).containsExactly("blender");
        assertThat(constraints.maxCookTimeMinutes()).isEqualTo(30);
    }
}
