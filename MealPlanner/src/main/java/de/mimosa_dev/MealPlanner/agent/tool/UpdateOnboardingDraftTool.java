package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftPatch;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FR-10: the onboarding agent saves the structured profile as it's collected, one call at a
 * time — any subset of fields may be included; omitted fields leave the existing draft value
 * untouched (see {@link OnboardingDraftPatch}'s javadoc). Scoped to the ONBOARDING scenario only
 * (AI-13) — see {@code ScenarioToolProvider}.
 */
@Component
public class UpdateOnboardingDraftTool implements AgentTool {

    public static final String NAME = "update_onboarding_draft";

    private final OnboardingDraftService draftService;
    private final ProductNormalizationService normalizationService;
    private final String description;

    public UpdateOnboardingDraftTool(
            OnboardingDraftService draftService,
            ProductNormalizationService normalizationService,
            @Value("classpath:prompts/onboarding/update-onboarding-draft-tool.txt") Resource descriptionResource) {
        this.draftService = draftService;
        this.normalizationService = normalizationService;
        this.description = ToolDescriptions.read(descriptionResource);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean stateChanging() {
        return true;
    }

    @Override
    public Tool definition() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("householdSize", Map.of("type", "integer", "description", "How many people this user cooks for"));
        properties.put("maxCookTimeWeekdayMinutes", Map.of("type", "integer",
                "description", "Maximum minutes the user wants to spend cooking on a weekday"));
        properties.put("excludedProductNames", Map.of("type", "array", "items", Map.of("type", "string"),
                "description", "Every allergy, intolerance, and disliked food the user mentioned. "
                        + "Pass an empty array (not omitted) once you've asked and the user has none."));
        properties.put("equipment", Map.of("type", "array", "items", Map.of("type", "string"),
                "description", "Kitchen equipment the user has available, e.g. \"oven\", \"blender\""));
        properties.put("freeDays", Map.of("type", "array",
                "items", Map.of("type", "string", "enum", List.of(
                        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")),
                "description", "Days the user doesn't want meal planning at all"));
        properties.put("goal", Map.of("type", "string",
                "enum", List.of("LOSE_WEIGHT", "GAIN_WEIGHT", "MAINTAIN", "VARIETY")));
        properties.put("weeklyBudget", Map.of("type", "number", "description", "Weekly grocery budget, if given"));
        properties.put("preferredStores", Map.of("type", "string"));
        properties.put("country", Map.of("type", "string", "description", "Two-letter country code"));
        properties.put("sex", Map.of("type", "string", "enum", List.of("MALE", "FEMALE"),
                "description", "Only ask if the user opted into calorie/macro goals (FR-13)"));
        properties.put("ageYears", Map.of("type", "integer"));
        properties.put("heightCm", Map.of("type", "number"));
        properties.put("weightKg", Map.of("type", "number"));
        properties.put("activityLevel", Map.of("type", "string",
                "enum", List.of("SEDENTARY", "LIGHT", "MODERATE", "ACTIVE", "VERY_ACTIVE")));

        Tool.InputSchema.Properties.Builder propertiesBuilder = Tool.InputSchema.Properties.builder();
        properties.forEach((name, schema) -> propertiesBuilder.putAdditionalProperty(name, JsonValue.from(schema)));

        return Tool.builder()
                .name(NAME)
                .description(description)
                .inputSchema(Tool.InputSchema.builder().properties(propertiesBuilder.build()).build())
                .build();
    }

    private record Input(
            Integer householdSize, Integer maxCookTimeWeekdayMinutes, List<String> excludedProductNames,
            List<String> equipment, List<String> freeDays, String goal, BigDecimal weeklyBudget,
            String preferredStores, String country, String sex, Integer ageYears, BigDecimal heightCm,
            BigDecimal weightKg, String activityLevel) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        Input parsed = input.convert(Input.class);

        Set<Long> excludedProductIds = parsed.excludedProductNames() == null ? null
                : parsed.excludedProductNames().stream()
                        .map(normalizationService::resolve)
                        .map(Product::getId)
                        .collect(Collectors.toSet());

        OnboardingDraftPatch patch = new OnboardingDraftPatch(
                parsed.householdSize(), parsed.maxCookTimeWeekdayMinutes(), excludedProductIds,
                parsed.equipment() == null ? null : Set.copyOf(parsed.equipment()),
                parsed.freeDays() == null ? null : Set.copyOf(parsed.freeDays()),
                parsed.goal(), parsed.weeklyBudget(), parsed.preferredStores(), parsed.country(),
                parsed.sex(), parsed.ageYears(), parsed.heightCm(), parsed.weightKg(), parsed.activityLevel());

        draftService.applyPatch(userId, patch);
        List<String> missing = draftService.missingMandatoryFields(userId);

        return missing.isEmpty()
                ? "Draft updated. All required fields are collected — call finalize_onboarding when ready."
                : "Draft updated. Still missing: " + String.join(", ", missing);
    }
}
