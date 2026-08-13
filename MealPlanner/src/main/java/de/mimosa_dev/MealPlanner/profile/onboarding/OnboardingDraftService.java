package de.mimosa_dev.MealPlanner.profile.onboarding;

import de.mimosa_dev.MealPlanner.profile.ActivityLevel;
import de.mimosa_dev.MealPlanner.profile.Goal;
import de.mimosa_dev.MealPlanner.profile.Sex;
import de.mimosa_dev.MealPlanner.profile.UserProfile;
import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * FR-10a ("agent gets the system prompt, the current draft, and the last 2-3 turns — not the
 * whole dialogue history") and FR-10b (resuming a lost/closed session from the saved draft).
 * This is the one place in the agent layer where conversation state is persisted server-side at
 * all — every other scenario is stateless per call (see {@link
 * de.mimosa_dev.MealPlanner.agent.AgentRunner}'s own javadoc).
 */
@Service
public class OnboardingDraftService {

    private static final int RECENT_TURNS_LIMIT = 3;

    private final OnboardingDraftRepository draftRepository;
    private final UserProfileRepository userProfileRepository;
    // A private, service-owned instance rather than Spring's auto-configured bean: this is a
    // purely internal (de)serialization detail with no HTTP-response-formatting concerns to
    // share, and @DataJpaTest slice contexts (used throughout this test suite) don't load
    // Jackson's auto-configuration at all, only @SpringBootTest/web-slice contexts do.
    // FAIL_ON_NULL_FOR_PRIMITIVES disabled: a brand-new draft's JSON is "{}" (no fields
    // collected yet), and OnboardingDraftData's one primitive field (allergiesCollected) would
    // otherwise reject that as an error rather than defaulting to false.
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    public OnboardingDraftService(OnboardingDraftRepository draftRepository, UserProfileRepository userProfileRepository) {
        this.draftRepository = draftRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public OnboardingDraftData currentDraft(Long userId) {
        return draftRepository.findByUserId(userId).map(this::readData).orElseGet(OnboardingDraftData::empty);
    }

    @Transactional(readOnly = true)
    public List<DialogueTurn> recentTurns(Long userId) {
        return draftRepository.findByUserId(userId).map(this::readTurns).orElseGet(List::of);
    }

    @Transactional
    public OnboardingDraftData applyPatch(Long userId, OnboardingDraftPatch patch) {
        OnboardingDraft draft = draftRepository.findByUserId(userId).orElseGet(() -> new OnboardingDraft(userId));
        OnboardingDraftData current = readData(draft);

        OnboardingDraftData merged = new OnboardingDraftData(
                orElse(patch.householdSize(), current.householdSize()),
                orElse(patch.maxCookTimeWeekdayMinutes(), current.maxCookTimeWeekdayMinutes()),
                orElse(patch.excludedProductIds(), current.excludedProductIds()),
                patch.excludedProductIds() != null || current.allergiesCollected(),
                orElse(patch.equipment(), current.equipment()),
                orElse(patch.freeDays(), current.freeDays()),
                orElse(patch.goal(), current.goal()),
                orElse(patch.weeklyBudget(), current.weeklyBudget()),
                orElse(patch.preferredStores(), current.preferredStores()),
                orElse(patch.country(), current.country()),
                orElse(patch.sex(), current.sex()),
                orElse(patch.ageYears(), current.ageYears()),
                orElse(patch.heightCm(), current.heightCm()),
                orElse(patch.weightKg(), current.weightKg()),
                orElse(patch.activityLevel(), current.activityLevel()));

        draft.setDraftJson(writeJson(merged));
        draftRepository.save(draft);
        return merged;
    }

    /**
     * FR-10a: builds the single message sent to the model this call — the draft-so-far, the
     * last few turns for conversational continuity, and the new user message. No change to
     * {@link de.mimosa_dev.MealPlanner.agent.AgentRunner} was needed for this: it's composed
     * entirely before {@code run()} is ever called.
     */
    @Transactional(readOnly = true)
    public String buildContextualMessage(Long userId, String newUserMessage) {
        OnboardingDraftData draft = currentDraft(userId);
        List<DialogueTurn> turns = recentTurns(userId);

        StringBuilder message = new StringBuilder();
        message.append("Current draft profile (JSON, fields not yet collected are null):\n")
                .append(writeJson(draft)).append("\n\n");
        if (!turns.isEmpty()) {
            message.append("Recent turns:\n");
            for (DialogueTurn turn : turns) {
                message.append(turn.role()).append(": ").append(turn.text()).append('\n');
            }
            message.append('\n');
        }
        message.append("User: ").append(newUserMessage);
        return message.toString();
    }

    @Transactional
    public void appendTurn(Long userId, String role, String text) {
        Optional<OnboardingDraft> existing = draftRepository.findByUserId(userId);
        if (existing.isEmpty() && userProfileRepository.findByUserId(userId).isPresent()) {
            // finalizeProfile deleted the draft this same call (the user's message was the one
            // that triggered finalize_onboarding) — onboarding is over, don't resurrect a draft
            // row just to log its closing turn.
            return;
        }
        OnboardingDraft draft = existing.orElseGet(() -> new OnboardingDraft(userId));
        List<DialogueTurn> turns = new ArrayList<>(readTurns(draft));
        turns.add(new DialogueTurn(role, text));
        List<DialogueTurn> trimmed = turns.size() > RECENT_TURNS_LIMIT
                ? turns.subList(turns.size() - RECENT_TURNS_LIMIT, turns.size())
                : turns;

        draft.setRecentTurnsJson(writeJson(trimmed));
        draftRepository.save(draft);
    }

    /**
     * FR-11: every mandatory field must be present before onboarding can complete. Declining
     * FR-13's optional TDEE inputs never blocks this — block "Б" (goals) just stays disabled.
     *
     * @throws IllegalStateException listing what's still missing (AI-21a: recoverable, goes
     *                                back to the model as an observation so it knows what to ask next)
     */
    @Transactional
    public UserProfile finalizeProfile(Long userId) {
        OnboardingDraftData data = currentDraft(userId);
        List<String> missing = missingMandatoryFields(userId);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Onboarding is missing required fields: " + String.join(", ", missing));
        }

        UserProfile profile = new UserProfile(userId, data.householdSize(), data.maxCookTimeWeekdayMinutes());
        if (data.excludedProductIds() != null) {
            profile.getExcludedProductIds().addAll(data.excludedProductIds());
        }
        if (data.equipment() != null) {
            profile.getEquipment().addAll(data.equipment());
        }
        if (data.freeDays() != null) {
            data.freeDays().stream().map(name -> DayOfWeek.valueOf(name.toUpperCase(Locale.ROOT)))
                    .forEach(profile.getFreeDays()::add);
        }
        if (data.goal() != null) {
            profile.setGoal(Goal.valueOf(data.goal().toUpperCase(Locale.ROOT)));
        }
        profile.setWeeklyBudget(data.weeklyBudget());
        profile.setPreferredStores(data.preferredStores());
        profile.setCountry(data.country());
        if (data.sex() != null) {
            profile.setSex(Sex.valueOf(data.sex().toUpperCase(Locale.ROOT)));
        }
        profile.setAgeYears(data.ageYears());
        profile.setHeightCm(data.heightCm());
        profile.setWeightKg(data.weightKg());
        if (data.activityLevel() != null) {
            profile.setActivityLevel(ActivityLevel.valueOf(data.activityLevel().toUpperCase(Locale.ROOT)));
        }
        // FR-13: goals turn on only once every TDEE input is actually present, never as a
        // side effect of merely finishing onboarding.
        profile.setGoalsEnabled(data.sex() != null && data.ageYears() != null && data.heightCm() != null
                && data.weightKg() != null && data.activityLevel() != null);

        UserProfile saved = userProfileRepository.save(profile);
        draftRepository.findByUserId(userId).ifPresent(draftRepository::delete);
        return saved;
    }

    /** What {@code update_onboarding_draft} reports back so the model knows what to ask next. */
    @Transactional(readOnly = true)
    public List<String> missingMandatoryFields(Long userId) {
        return missingMandatoryFields(currentDraft(userId));
    }

    private static List<String> missingMandatoryFields(OnboardingDraftData data) {
        List<String> missing = new ArrayList<>();
        if (data.householdSize() == null) {
            missing.add("household size");
        }
        if (data.maxCookTimeWeekdayMinutes() == null) {
            missing.add("maximum weekday cook time");
        }
        if (data.equipment() == null) {
            missing.add("available equipment");
        }
        if (!data.allergiesCollected()) {
            missing.add("allergies and foods the user won't eat");
        }
        return missing;
    }

    private OnboardingDraftData readData(OnboardingDraft draft) {
        try {
            return objectMapper.readValue(draft.getDraftJson(), OnboardingDraftData.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Corrupt onboarding draft for user " + draft.getUserId(), e);
        }
    }

    private List<DialogueTurn> readTurns(OnboardingDraft draft) {
        try {
            return objectMapper.readValue(draft.getRecentTurnsJson(), new TypeReference<List<DialogueTurn>>() {
            });
        } catch (JacksonException e) {
            throw new IllegalStateException("Corrupt onboarding turns for user " + draft.getUserId(), e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize onboarding draft data", e);
        }
    }

    private static <T> T orElse(T patchValue, T currentValue) {
        return patchValue != null ? patchValue : currentValue;
    }
}
