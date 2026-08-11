package de.mimosa_dev.MealPlanner.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * FR-60/FR-63: a soft, accumulated signal from a rejection — lowers a matching future
 * candidate's score (see {@link RecipeCandidateScorer}), never excludes it outright. Matched
 * by recipe name (Phase A simplification: FR-60's five reasons target dish/category/ingredient
 * granularity, which would need category resolution and ingredient-overlap heuristics at
 * suggestion time to implement fully — {@code recipeName} is denormalized here rather than
 * requiring a join in the scorer's hot path).
 */
@Entity
@Table(name = "preference_signal")
public class PreferenceSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "recipe_name", nullable = false)
    private String recipeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private RejectionReason reason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected PreferenceSignal() {
    }

    public PreferenceSignal(Long userId, Long recipeId, String recipeName, RejectionReason reason) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public RejectionReason getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
