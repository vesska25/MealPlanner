package de.mimosa_dev.MealPlanner.profile.onboarding;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.profile.UserProfile;
import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(OnboardingDraftService.class)
class OnboardingDraftServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private OnboardingDraftService draftService;

    @Autowired
    private OnboardingDraftRepository draftRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void applyPatchMergesFieldsAcrossMultipleCallsWithoutOverwritingUntouchedOnes() {
        draftService.applyPatch(USER_ID, patch(2, null, null));
        draftService.applyPatch(USER_ID, patch(null, 30, null));

        OnboardingDraftData draft = draftService.currentDraft(USER_ID);

        assertThat(draft.householdSize()).isEqualTo(2);
        assertThat(draft.maxCookTimeWeekdayMinutes()).isEqualTo(30);
    }

    @Test
    void anEmptyExcludedProductSetIsDistinguishedFromNotAskedYet() {
        assertThat(draftService.currentDraft(USER_ID).allergiesCollected()).isFalse();

        draftService.applyPatch(USER_ID, new OnboardingDraftPatch(
                null, null, Set.of(), null, null, null, null, null, null, null, null, null, null, null));

        assertThat(draftService.currentDraft(USER_ID).allergiesCollected()).isTrue();
        assertThat(draftService.currentDraft(USER_ID).excludedProductIds()).isEmpty();
    }

    @Test
    void buildContextualMessageIncludesTheDraftRecentTurnsAndNewMessage() {
        draftService.applyPatch(USER_ID, patch(3, null, null));
        draftService.appendTurn(USER_ID, "user", "I cook for 3 people");
        draftService.appendTurn(USER_ID, "agent", "Got it, 3 people.");

        String message = draftService.buildContextualMessage(USER_ID, "no allergies");

        assertThat(message).contains("\"householdSize\":3");
        assertThat(message).contains("I cook for 3 people");
        assertThat(message).contains("Got it, 3 people.");
        assertThat(message).contains("User: no allergies");
    }

    @Test
    void appendTurnKeepsOnlyTheMostRecentThree() {
        draftService.appendTurn(USER_ID, "user", "one");
        draftService.appendTurn(USER_ID, "agent", "two");
        draftService.appendTurn(USER_ID, "user", "three");
        draftService.appendTurn(USER_ID, "agent", "four");

        assertThat(draftService.recentTurns(USER_ID)).extracting(DialogueTurn::text)
                .containsExactly("two", "three", "four");
    }

    @Test
    void finalizeProfileFailsWithMissingFieldsWhenMandatoryDataIsIncomplete() {
        draftService.applyPatch(USER_ID, patch(2, null, null)); // maxCookTime and allergies missing

        assertThatThrownBy(() -> draftService.finalizeProfile(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum weekday cook time")
                .hasMessageContaining("allergies");
    }

    @Test
    void finalizeProfileCreatesTheRealProfileAndDeletesTheDraftOnceEverythingIsCollected() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        draftService.applyPatch(USER_ID, new OnboardingDraftPatch(
                4, 45, Set.of(milk.getId()), Set.of("oven"), null, null, null, null, null, null, null, null, null, null));

        UserProfile profile = draftService.finalizeProfile(USER_ID);

        assertThat(profile.getHouseholdSize()).isEqualTo(4);
        assertThat(profile.getExcludedProductIds()).containsExactly(milk.getId());
        assertThat(userProfileRepository.findByUserId(USER_ID)).isPresent();
        assertThat(draftRepository.findByUserId(USER_ID)).isEmpty();
        // FR-13: no TDEE inputs were ever given, so goals stay off rather than defaulting on.
        assertThat(profile.isGoalsEnabled()).isFalse();
    }

    @Test
    void appendTurnDoesNotResurrectADraftAfterOnboardingIsAlreadyFinalized() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        draftService.applyPatch(USER_ID, new OnboardingDraftPatch(
                4, 45, Set.of(milk.getId()), Set.of("oven"), null, null, null, null, null, null, null, null, null, null));
        draftService.finalizeProfile(USER_ID);

        draftService.appendTurn(USER_ID, "user", "one more message after finishing");

        assertThat(draftRepository.findByUserId(USER_ID)).isEmpty();
    }

    private static OnboardingDraftPatch patch(Integer householdSize, Integer maxCookTimeWeekdayMinutes, Set<Long> excludedProductIds) {
        return new OnboardingDraftPatch(
                householdSize, maxCookTimeWeekdayMinutes, excludedProductIds, null, null, null,
                (BigDecimal) null, null, null, null, null, null, null, null);
    }
}
