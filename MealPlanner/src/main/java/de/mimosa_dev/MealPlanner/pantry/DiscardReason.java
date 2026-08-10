package de.mimosa_dev.MealPlanner.pantry;

/** Why a pantry item was marked discarded (FR-23); feeds future heuristic corrections. */
public enum DiscardReason {
    EXPIRED_EARLY,
    DIDNT_COOK_IN_TIME,
    BOUGHT_TOO_MUCH
}
