package de.mimosa_dev.MealPlanner.account;

import de.mimosa_dev.MealPlanner.account.dto.AccountExportResponse;
import de.mimosa_dev.MealPlanner.account.dto.AgentRunExport;
import de.mimosa_dev.MealPlanner.account.dto.CookedDishExport;
import de.mimosa_dev.MealPlanner.account.dto.PantryItemExport;
import de.mimosa_dev.MealPlanner.account.dto.RecipeExport;
import de.mimosa_dev.MealPlanner.account.dto.RecipeIngredientExport;
import de.mimosa_dev.MealPlanner.agent.AgentRun;
import de.mimosa_dev.MealPlanner.agent.AgentRunRepository;
import de.mimosa_dev.MealPlanner.auth.AppUser;
import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.cooking.CookedDish;
import de.mimosa_dev.MealPlanner.cooking.CookedDishRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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

    public AccountService(
            AppUserRepository appUserRepository,
            PantryItemRepository pantryItemRepository,
            RecipeRepository recipeRepository,
            CookedDishRepository cookedDishRepository,
            AgentRunRepository agentRunRepository) {
        this.appUserRepository = appUserRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.recipeRepository = recipeRepository;
        this.cookedDishRepository = cookedDishRepository;
        this.agentRunRepository = agentRunRepository;
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

        return new AccountExportResponse(user.getEmail(), pantryItems, recipes, cookedDishes, agentRuns);
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
        return new RecipeExport(
                recipe.getId(), recipe.getName(), recipe.getCookTimeMinutes(), recipe.getBasePortions(),
                recipe.getRequiredEquipment(), ingredients);
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
}
