package de.mimosa_dev.MealPlanner.shoppinglist;

import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestion;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * FR-40 to FR-45b (PRD step 10, Phase B). List generation needs no model creativity at all —
 * unlike recipe proposal, both blocks are 100% deterministic from the active suggestion and
 * current pantry state, so "the model" here is only ever presenting what Java already computed.
 */
@Service
public class ShoppingListService {

    // FR-41: a proxy for "1-2 days of food remaining" — a true days-of-supply estimate would
    // need consumption-rate/meal-frequency modeling this codebase doesn't have anywhere. Instead:
    // every active non-staple pantry item expiring imminently (or none at all) reads as "running low".
    private static final int LOW_STOCK_HORIZON_DAYS = 2;

    private final RecipeSuggestionRepository recipeSuggestionRepository;
    private final RecipeRepository recipeRepository;
    private final PantryItemRepository pantryItemRepository;
    private final PantryService pantryService;
    private final ProductNormalizationService normalizationService;
    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;

    public ShoppingListService(
            RecipeSuggestionRepository recipeSuggestionRepository,
            RecipeRepository recipeRepository,
            PantryItemRepository pantryItemRepository,
            PantryService pantryService,
            ProductNormalizationService normalizationService,
            ShoppingListRepository shoppingListRepository,
            ShoppingListItemRepository shoppingListItemRepository) {
        this.recipeSuggestionRepository = recipeSuggestionRepository;
        this.recipeRepository = recipeRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.pantryService = pantryService;
        this.normalizationService = normalizationService;
        this.shoppingListRepository = shoppingListRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
    }

    public record GeneratedShoppingList(ShoppingList shoppingList, boolean pantryRunningLow) {
    }

    public record ItemResolution(Long itemId, ShoppingListItemStatus status) {
    }

    public record ExtraPurchase(String productName, BigDecimal quantity, Unit unit) {
    }

    /**
     * FR-40/FR-42: derived entirely from the current active suggestion (if any) and pantry
     * state — no active suggestion just means an empty list, not an error.
     */
    @Transactional
    public GeneratedShoppingList generate(Long userId) {
        ShoppingList shoppingList = new ShoppingList(userId);

        Optional<RecipeSuggestion> activeSuggestion =
                recipeSuggestionRepository.findByUserIdAndStatus(userId, RecipeSuggestionStatus.ACTIVE);
        if (activeSuggestion.isPresent()) {
            Recipe recipe = recipeRepository.findWithIngredientsById(activeSuggestion.get().getRecipeId())
                    .orElseThrow(() -> new NoSuchElementException("Recipe " + activeSuggestion.get().getRecipeId() + " not found"));
            for (RecipeIngredientEntity ingredient : recipe.getIngredients()) {
                Product product = ingredient.getProduct();
                if (product.isStaple()) {
                    shoppingList.addItem(new ShoppingListItem(
                            userId, product, ingredient.getQuantity(), ingredient.getUnit(), ShoppingListBlock.CHECK_MAYBE_OUT));
                    continue;
                }
                BigDecimal available = pantryItemRepository.sumQuantityByUserAndProductAndUnitAndStatus(
                        userId, product.getId(), ingredient.getUnit(), PantryItemStatus.ACTIVE);
                BigDecimal missing = ingredient.getQuantity().subtract(available);
                if (missing.signum() > 0) {
                    shoppingList.addItem(new ShoppingListItem(
                            userId, product, missing, ingredient.getUnit(), ShoppingListBlock.DEFINITELY_NEED));
                }
            }
        }

        ShoppingList saved = shoppingListRepository.save(shoppingList);
        return new GeneratedShoppingList(saved, isPantryRunningLow(userId));
    }

    /** Public since step 12: reused directly by TelegramNotificationScheduler's shopping-reminder check (FR-81). */
    public boolean isPantryRunningLow(Long userId) {
        List<PantryItem> active = pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(userId, PantryItemStatus.ACTIVE)
                .stream()
                .filter(item -> !item.getProduct().isStaple())
                .toList();
        if (active.isEmpty()) {
            return true;
        }
        LocalDate horizon = LocalDate.now().plusDays(LOW_STOCK_HORIZON_DAYS);
        return active.stream().allMatch(item -> !item.getExpiresAt().isAfter(horizon));
    }

    /**
     * FR-44/FR-45/FR-45b: {@code PURCHASED}/{@code ALREADY_HAVE} add the item to pantry as an
     * estimate (the list's suggested quantity, not a precisely counted one); the other statuses
     * touch nothing. Free-text extras (not on the list at all) get a real, non-estimated
     * quantity instead, same treatment {@code AddPantryStockTool} already gives a direct purchase.
     */
    @Transactional
    public void resolveItems(Long userId, List<ItemResolution> resolutions, List<ExtraPurchase> extras) {
        for (ItemResolution resolution : resolutions) {
            ShoppingListItem item = shoppingListItemRepository.findByIdAndUserId(resolution.itemId(), userId)
                    .orElseThrow(() -> new NoSuchElementException("Shopping list item " + resolution.itemId() + " not found"));
            item.resolve(resolution.status());
            if (resolution.status() == ShoppingListItemStatus.PURCHASED
                    || resolution.status() == ShoppingListItemStatus.ALREADY_HAVE) {
                pantryService.addStock(userId, item.getProduct(), item.getQuantity(), item.getUnit(), LocalDate.now(), true);
            }
            shoppingListItemRepository.save(item);
        }

        for (ExtraPurchase extra : extras) {
            Product product = normalizationService.resolve(extra.productName());
            pantryService.addStock(userId, product, extra.quantity(), extra.unit(), LocalDate.now(), false);
        }
    }
}
