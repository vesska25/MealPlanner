package de.mimosa_dev.MealPlanner.account;

import de.mimosa_dev.MealPlanner.account.dto.AccountExportResponse;
import de.mimosa_dev.MealPlanner.account.dto.AgentRunExport;
import de.mimosa_dev.MealPlanner.account.dto.CookedDishExport;
import de.mimosa_dev.MealPlanner.account.dto.PantryItemExport;
import de.mimosa_dev.MealPlanner.account.dto.RecipeExport;
import de.mimosa_dev.MealPlanner.account.dto.RecipeIngredientExport;
import de.mimosa_dev.MealPlanner.account.dto.UserProfileExport;
import de.mimosa_dev.MealPlanner.agent.AgentRun;
import de.mimosa_dev.MealPlanner.agent.AgentRunRepository;
import de.mimosa_dev.MealPlanner.auth.AppUser;
import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.cooking.CookedDish;
import de.mimosa_dev.MealPlanner.cooking.CookedDishRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.profile.UserProfile;
import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FR-04 (export everything a user owns as JSON) and FR-05 (cascading, irreversible deletion).
 * Deletion is a single {@code deleteById} — the DB-level {@code ON DELETE CASCADE} added in V8
 * removes every owned row across pantry, recipes, cooked dishes, and agent logs.
 */
@Service
public class AccountService {

    private final AppUserRepository appUserRepository;
    private final PantryItemRepository pantryItemRepository;
    private final RecipeRepository recipeRepository;
    private final CookedDishRepository cookedDishRepository;
    private final AgentRunRepository agentRunRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProductRepository productRepository;

    public AccountService(
            AppUserRepository appUserRepository,
            PantryItemRepository pantryItemRepository,
            RecipeRepository recipeRepository,
            CookedDishRepository cookedDishRepository,
            AgentRunRepository agentRunRepository,
            UserProfileRepository userProfileRepository,
            ProductRepository productRepository) {
        this.appUserRepository = appUserRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.recipeRepository = recipeRepository;
        this.cookedDishRepository = cookedDishRepository;
        this.agentRunRepository = agentRunRepository;
        this.userProfileRepository = userProfileRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public AccountExportResponse exportData(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User " + userId + " not found"));

        List<PantryItemExport> pantryItems = pantryItemRepository.findByUserId(userId).stream()
                .map(AccountService::toExport)
                .toList();
        List<RecipeExport> recipes = recipeRepository.findByUserId(userId).stream()
                .map(AccountService::toExport)
                .toList();
        List<CookedDishExport> cookedDishes = cookedDishRepository.findByUserId(userId).stream()
                .map(AccountService::toExport)
                .toList();
        List<AgentRunExport> agentRuns = agentRunRepository.findByUserId(userId).stream()
                .map(AccountService::toExport)
                .toList();
        UserProfileExport profile = userProfileRepository.findByUserId(userId)
                .map(this::toExport)
                .orElse(null);

        return new AccountExportResponse(user.getEmail(), pantryItems, recipes, cookedDishes, agentRuns, profile);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        appUserRepository.deleteById(userId);
    }

    private static PantryItemExport toExport(PantryItem item) {
        return new PantryItemExport(
                item.getId(), item.getProduct().getCanonicalName(), item.getQuantity(), item.getUnit(),
                item.getPurchasedAt(), item.getExpiresAt(), item.getStatus().name(),
                item.getDiscardReason() == null ? null : item.getDiscardReason().name());
    }

    private static RecipeExport toExport(Recipe recipe) {
        List<RecipeIngredientExport> ingredients = recipe.getIngredients().stream()
                .map(ingredient -> new RecipeIngredientExport(
                        ingredient.getProduct().getCanonicalName(), ingredient.getQuantity(), ingredient.getUnit()))
                .toList();
        // Copy out of the lazy Hibernate collection while the session is still open (same
        // reason ingredients is materialized via .toList() above) — otherwise this stays an
        // uninitialized proxy that throws LazyInitializationException when Jackson serializes
        // the response after the @Transactional method (and its session) has already returned.
        Set<String> requiredEquipment = Set.copyOf(recipe.getRequiredEquipment());
        return new RecipeExport(
                recipe.getId(), recipe.getName(), recipe.getCookTimeMinutes(), recipe.getBasePortions(),
                requiredEquipment, ingredients);
    }

    private static CookedDishExport toExport(CookedDish dish) {
        return new CookedDishExport(
                dish.getId(), dish.getRecipe().getId(), dish.getCategory().name(),
                dish.getTotalPortions(), dish.getPortionsRemaining(),
                dish.getKcalPerPortion(), dish.getProteinPerPortion(), dish.getFatPerPortion(), dish.getCarbsPerPortion(),
                dish.getCookedAt(), dish.getExpiresAt(), dish.getStatus().name());
    }

    private static AgentRunExport toExport(AgentRun run) {
        return new AgentRunExport(
                run.getId(), run.getScenario().name(), run.getTrigger(), run.getStatus().name(),
                run.getIterationCount(), run.getStartedAt(), run.getFinishedAt());
    }

    private UserProfileExport toExport(UserProfile profile) {
        Set<String> excludedProductNames = productRepository.findAllById(profile.getExcludedProductIds()).stream()
                .map(Product::getCanonicalName)
                .collect(Collectors.toSet());
        Set<String> freeDays = profile.getFreeDays().stream().map(Enum::name).collect(Collectors.toSet());
        return new UserProfileExport(
                profile.getHouseholdSize(), profile.getMaxCookTimeWeekdayMinutes(), excludedProductNames,
                profile.getEquipment(), freeDays,
                profile.getGoal() == null ? null : profile.getGoal().name(),
                profile.getWeeklyBudget(), profile.getPreferredStores(), profile.getCountry(),
                profile.getSex() == null ? null : profile.getSex().name(),
                profile.getAgeYears(), profile.getHeightCm(), profile.getWeightKg(),
                profile.getActivityLevel() == null ? null : profile.getActivityLevel().name(),
                profile.isGoalsEnabled());
    }
}
