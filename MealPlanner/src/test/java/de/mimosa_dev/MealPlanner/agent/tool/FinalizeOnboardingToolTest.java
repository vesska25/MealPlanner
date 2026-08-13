package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftPatch;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({FinalizeOnboardingTool.class, OnboardingDraftService.class})
class FinalizeOnboardingToolTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private FinalizeOnboardingTool tool;

    @Autowired
    private OnboardingDraftService draftService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void raisesARecoverableErrorWhenRequiredFieldsAreMissing() {
        assertThatThrownBy(() -> tool.execute(USER_ID, JsonValue.from(Map.of())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(userProfileRepository.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void createsTheProfileAndTellsTheModelToOfferTheStartingScenarioChoice() {
        draftService.applyPatch(USER_ID, new OnboardingDraftPatch(
                2, 30, Set.of(), Set.of(), null, null, null, null, null, null, null, null, null, null));

        String result = tool.execute(USER_ID, JsonValue.from(Map.of()));

        assertThat(result).contains("cooking from what they have at home");
        assertThat(userProfileRepository.findByUserId(USER_ID)).isPresent();
    }

    @Test
    void toolDefinitionIsWellFormed() {
        assertThat(tool.definition().name()).isEqualTo(tool.name());
        assertThat(tool.definition().description()).isPresent();
    }
}
