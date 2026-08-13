package de.mimosa_dev.MealPlanner.profile;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileRepositoryTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void savesAndReloadsAFullyPopulatedProfileWithItsCollections() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();

        UserProfile profile = new UserProfile(USER_ID, 2, 45);
        profile.setGoal(Goal.LOSE_WEIGHT);
        profile.setSex(Sex.FEMALE);
        profile.setAgeYears(29);
        profile.setHeightCm(new BigDecimal("165.5"));
        profile.setWeightKg(new BigDecimal("60.0"));
        profile.setActivityLevel(ActivityLevel.MODERATE);
        profile.setGoalsEnabled(true);
        profile.getExcludedProductIds().add(milk.getId());
        profile.getEquipment().add("oven");
        profile.getFreeDays().add(DayOfWeek.SUNDAY);

        Long id = userProfileRepository.saveAndFlush(profile).getId();
        entityManager.clear();

        UserProfile reloaded = userProfileRepository.findById(id).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(USER_ID);
        assertThat(reloaded.getHouseholdSize()).isEqualTo(2);
        assertThat(reloaded.getGoal()).isEqualTo(Goal.LOSE_WEIGHT);
        assertThat(reloaded.getSex()).isEqualTo(Sex.FEMALE);
        assertThat(reloaded.isGoalsEnabled()).isTrue();
        assertThat(reloaded.getExcludedProductIds()).containsExactly(milk.getId());
        assertThat(reloaded.getEquipment()).containsExactly("oven");
        assertThat(reloaded.getFreeDays()).containsExactly(DayOfWeek.SUNDAY);
    }

    @Test
    void findByUserIdReturnsEmptyWhenOnboardingHasNotBeenCompleted() {
        assertThat(userProfileRepository.findByUserId(USER_ID)).isEmpty();
    }
}
