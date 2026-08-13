package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * FR-11/FR-15: creates the real {@link de.mimosa_dev.MealPlanner.profile.UserProfile} once every
 * mandatory field has been collected, ending onboarding. Scoped to the ONBOARDING scenario only.
 */
@Component
public class FinalizeOnboardingTool implements AgentTool {

    public static final String NAME = "finalize_onboarding";

    private final OnboardingDraftService draftService;
    private final String description;

    public FinalizeOnboardingTool(
            OnboardingDraftService draftService,
            @Value("classpath:prompts/onboarding/finalize-onboarding-tool.txt") Resource descriptionResource) {
        this.draftService = draftService;
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
        return Tool.builder()
                .name(NAME)
                .description(description)
                .inputSchema(Tool.InputSchema.builder().build())
                .build();
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        // IllegalStateException (missing fields) is a RuntimeException, so it's returned to the
        // model as an observation per AI-21a — no explicit catch needed here.
        draftService.finalizeProfile(userId);
        return "Onboarding complete. Ask the user which they'd like to start with: cooking from "
                + "what they have at home, or going shopping.";
    }
}
