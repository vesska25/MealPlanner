package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import({UpdateOnboardingDraftTool.class, OnboardingDraftService.class, ProductNormalizationService.class})
class UpdateOnboardingDraftToolTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private UpdateOnboardingDraftTool tool;

    @Autowired
    private OnboardingDraftService draftService;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void reportsWhatIsStillMissingAfterAPartialUpdate() {
        String result = tool.execute(USER_ID, JsonValue.from(Map.of("householdSize", 2)));

        assertThat(result).contains("Still missing");
        assertThat(result).contains("maximum weekday cook time");
        assertThat(result).contains("allergies");
    }

    @Test
    void anEmptyExcludedProductNamesListMarksAllergiesAsCollected() {
        tool.execute(USER_ID, JsonValue.from(Map.of("excludedProductNames", List.of())));

        assertThat(draftService.currentDraft(USER_ID).allergiesCollected()).isTrue();
    }

    @Test
    void freeTextAllergyNamesAreResolvedToProductIds() {
        tool.execute(USER_ID, JsonValue.from(Map.of("excludedProductNames", List.of("milk"))));

        assertThat(draftService.currentDraft(USER_ID).excludedProductIds()).isNotEmpty();
    }

    @Test
    void confirmsWhenEverythingRequiredHasBeenCollected() {
        tool.execute(USER_ID, JsonValue.from(Map.of(
                "householdSize", 2, "maxCookTimeWeekdayMinutes", 30, "equipment", List.of("oven"))));

        String result = tool.execute(USER_ID, JsonValue.from(Map.of("excludedProductNames", List.of())));

        assertThat(result).contains("All required fields are collected");
    }

    @Test
    void toolDefinitionIsWellFormed() {
        assertThat(tool.definition().name()).isEqualTo(tool.name());
        assertThat(tool.definition().description()).isPresent();
    }
}
