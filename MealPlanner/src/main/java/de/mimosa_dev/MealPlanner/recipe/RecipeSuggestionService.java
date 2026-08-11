package de.mimosa_dev.MealPlanner.recipe;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * FR-24's suggestion lifecycle (PRD 9.1 step 10, Phase A). Exactly one {@code ACTIVE} row per
 * user at a time — every transition through this service maintains that invariant rather than
 * relying on callers to check first.
 */
@Service
public class RecipeSuggestionService {

    private final RecipeSuggestionRepository recipeSuggestionRepository;
    private final PreferenceSignalRepository preferenceSignalRepository;
    private final RecipeRepository recipeRepository;

    public RecipeSuggestionService(
            RecipeSuggestionRepository recipeSuggestionRepository,
            PreferenceSignalRepository preferenceSignalRepository,
            RecipeRepository recipeRepository) {
        this.recipeSuggestionRepository = recipeSuggestionRepository;
        this.preferenceSignalRepository = preferenceSignalRepository;
        this.recipeRepository = recipeRepository;
    }

    /** Expires whatever was previously {@code ACTIVE} for this user, then activates {@code recipeId}. */
    @Transactional
    public RecipeSuggestion activate(Long userId, Long recipeId, BigDecimal score) {
        recipeSuggestionRepository.findByUserIdAndStatus(userId, RecipeSuggestionStatus.ACTIVE)
                .ifPresent(active -> {
                    active.expire();
                    recipeSuggestionRepository.save(active);
                });
        return recipeSuggestionRepository.save(new RecipeSuggestion(userId, recipeId, score));
    }

    /**
     * FR-60/FR-61: rejects the current {@code ACTIVE} suggestion, which must be the one for
     * {@code recipeId} — rejecting anything else (already resolved, or a different recipe
     * entirely) is a caller error. FR-62: {@code NOT_TODAY} never becomes a {@link PreferenceSignal}.
     */
    @Transactional
    public void reject(Long userId, Long recipeId, RejectionReason reason) {
        RecipeSuggestion active = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(userId, recipeId, RecipeSuggestionStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException(
                        "No active suggestion for recipe " + recipeId + " and user " + userId));
        active.reject(reason);
        recipeSuggestionRepository.save(active);

        if (reason != RejectionReason.NOT_TODAY) {
            String recipeName = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new NoSuchElementException("Recipe " + recipeId + " not found"))
                    .getName();
            preferenceSignalRepository.save(new PreferenceSignal(userId, recipeId, recipeName, reason));
        }
    }

    /** No-op if no matching {@code ACTIVE} suggestion exists — most cookings won't have one. */
    @Transactional
    public void accept(Long userId, Long recipeId) {
        Optional<RecipeSuggestion> active = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(userId, recipeId, RecipeSuggestionStatus.ACTIVE);
        if (active.isPresent()) {
            active.get().accept();
            recipeSuggestionRepository.save(active.get());
        }
    }
}
