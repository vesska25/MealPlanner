package de.mimosa_dev.MealPlanner.agent.tool;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeCandidate;
import de.mimosa_dev.MealPlanner.recipe.RecipeCandidateScorer;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredient;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.recipe.RecipeValidationResult;
import de.mimosa_dev.MealPlanner.recipe.RecipeValidator;
import de.mimosa_dev.MealPlanner.recipe.RecipeViolation;
import de.mimosa_dev.MealPlanner.recipe.UserConstraints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Where FR-34 (model generates recipes) meets AI-08/AI-09 (Java validates and ranks them
 * before anything is shown): the model submits candidates as a tool call, and this tool runs
 * every one of them through {@link RecipeValidator} and {@link RecipeCandidateScorer} before
 * any of it becomes a persisted {@link Recipe}.
 */
@Component
public class ProposeRecipeCandidatesTool implements AgentTool {

    public static final String NAME = "propose_recipe_candidates";

    private final ProductNormalizationService normalizationService;
    private final RecipeValidator recipeValidator;
    private final RecipeCandidateScorer scorer;
    private final RecipeRepository recipeRepository;
    private final RecipeSuggestionService recipeSuggestionService;
    private final String description;

    public ProposeRecipeCandidatesTool(
            ProductNormalizationService normalizationService,
            RecipeValidator recipeValidator,
            RecipeCandidateScorer scorer,
            RecipeRepository recipeRepository,
            RecipeSuggestionService recipeSuggestionService,
            @Value("classpath:prompts/meal-planning/propose-recipe-candidates-tool.txt") Resource descriptionResource) {
        this.normalizationService = normalizationService;
        this.recipeValidator = recipeValidator;
        this.scorer = scorer;
        this.recipeRepository = recipeRepository;
        this.recipeSuggestionService = recipeSuggestionService;
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
        Map<String, Object> ingredientSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "productName", Map.of("type", "string",
                                "description", "Canonical or free-text product name"),
                        "quantity", Map.of("type", "number",
                                "description", "Amount needed, as a plain number"),
                        "unit", Map.of("type", "string",
                                "enum", List.of("GRAM", "MILLILITER", "PIECE"))),
                "required", List.of("productName", "quantity", "unit"));

        Map<String, Object> candidateSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string", "description", "Recipe name"),
                        "ingredients", Map.of("type", "array", "items", ingredientSchema),
                        "requiredEquipment", Map.of("type", "array",
                                "items", Map.of("type", "string"),
                                "description", "Equipment this recipe needs, e.g. \"oven\""),
                        "cookTimeMinutes", Map.of("type", "integer",
                                "description", "Total time to cook, in minutes"),
                        "basePortions", Map.of("type", "integer",
                                "description", "How many portions this recipe makes as written — "
                                        + "the ingredient quantities above are for this many portions")),
                "required", List.of("name", "ingredients", "requiredEquipment", "cookTimeMinutes", "basePortions"));

        return Tool.builder()
                .name(NAME)
                .description(description)
                .inputSchema(Tool.InputSchema.builder()
                        .properties(Tool.InputSchema.Properties.builder()
                                .putAdditionalProperty("candidates", JsonValue.from(Map.of(
                                        "type", "array",
                                        "items", candidateSchema,
                                        "description", "One or more recipe ideas to validate and rank")))
                                .build())
                        .required(List.of("candidates"))
                        .build())
                .build();
    }

    private record IngredientInput(String productName, BigDecimal quantity, Unit unit) {
    }

    private record CandidateInput(
            String name, List<IngredientInput> ingredients, Set<String> requiredEquipment,
            int cookTimeMinutes, int basePortions) {
    }

    private record Input(List<CandidateInput> candidates) {
    }

    private record ScoredCandidate(Long recipeId, String name, double score) {
    }

    @Override
    public String execute(Long userId, JsonValue input) {
        List<CandidateInput> candidates = input.convert(Input.class).candidates();
        if (candidates.isEmpty()) {
            return "No candidates were submitted.";
        }

        List<ScoredCandidate> accepted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();

        for (CandidateInput candidateInput : candidates) {
            List<Product> resolvedProducts = new ArrayList<>();
            List<RecipeIngredient> ingredients = new ArrayList<>();
            for (IngredientInput ingredientInput : candidateInput.ingredients()) {
                Product product = normalizationService.resolve(ingredientInput.productName());
                resolvedProducts.add(product);
                ingredients.add(new RecipeIngredient(product.getId(), ingredientInput.quantity(), ingredientInput.unit()));
            }

            RecipeCandidate candidate = new RecipeCandidate(
                    candidateInput.name(), ingredients, candidateInput.requiredEquipment(), candidateInput.cookTimeMinutes());

            RecipeValidationResult validation = recipeValidator.validate(userId, candidate, UserConstraints.defaults());
            if (!validation.valid()) {
                String reasons = validation.violations().stream()
                        .map(RecipeViolation::message)
                        .collect(Collectors.joining("; "));
                rejected.add(candidateInput.name() + ": " + reasons);
                continue;
            }

            double score = scorer.score(userId, candidate);

            Recipe recipe = new Recipe(userId, candidateInput.name(), candidateInput.cookTimeMinutes(),
                    candidateInput.basePortions(), candidateInput.requiredEquipment());
            for (int i = 0; i < ingredients.size(); i++) {
                recipe.addIngredient(new RecipeIngredientEntity(
                        resolvedProducts.get(i), ingredients.get(i).quantity(), ingredients.get(i).unit()));
            }
            Recipe saved = recipeRepository.save(recipe);

            accepted.add(new ScoredCandidate(saved.getId(), candidateInput.name(), score));
        }

        // FR-31a: ranking never depends on submission order — ties broken deterministically by name.
        accepted.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed()
                .thenComparing(ScoredCandidate::name));

        // FR-24: the top-ranked candidate becomes the new active suggestion (expiring whatever
        // was previously active) — the rest stay as plain, un-life-cycled alternatives, visible
        // only in the text reply below.
        if (!accepted.isEmpty()) {
            ScoredCandidate top = accepted.get(0);
            recipeSuggestionService.activate(userId, top.recipeId(), BigDecimal.valueOf(top.score()));
        }

        return formatResult(accepted, rejected);
    }

    private static String formatResult(List<ScoredCandidate> accepted, List<String> rejected) {
        StringBuilder result = new StringBuilder();
        if (!accepted.isEmpty()) {
            result.append("Ranked candidates:\n");
            int rank = 1;
            for (ScoredCandidate candidate : accepted) {
                result.append("%d. %s (score %.2f) - saved as recipe #%d%n"
                        .formatted(rank++, candidate.name(), candidate.score(), candidate.recipeId()));
            }
        }
        if (!rejected.isEmpty()) {
            if (!accepted.isEmpty()) {
                result.append("\n");
            }
            result.append("Rejected:\n");
            for (String reason : rejected) {
                result.append("- ").append(reason).append("\n");
            }
        }
        return result.toString().strip();
    }
}
