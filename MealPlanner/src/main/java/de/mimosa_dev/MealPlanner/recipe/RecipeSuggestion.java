package de.mimosa_dev.MealPlanner.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * FR-24: a dish suggestion's lifecycle. Kept as a plain {@code Long} reference to
 * {@code Recipe} (not a JPA association), matching this codebase's established choice of not
 * adding object-graph navigation for entities that don't need to traverse it.
 */
@Entity
@Table(name = "recipe_suggestion")
public class RecipeSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecipeSuggestionStatus status;

    @Column(name = "score", nullable = false)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason")
    private RejectionReason rejectionReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected RecipeSuggestion() {
    }

    public RecipeSuggestion(Long userId, Long recipeId, BigDecimal score) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.score = score;
        this.status = RecipeSuggestionStatus.ACTIVE;
    }

    public void expire() {
        this.status = RecipeSuggestionStatus.EXPIRED;
        this.resolvedAt = Instant.now();
    }

    public void reject(RejectionReason reason) {
        this.status = RecipeSuggestionStatus.REJECTED;
        this.rejectionReason = reason;
        this.resolvedAt = Instant.now();
    }

    public void accept() {
        this.status = RecipeSuggestionStatus.ACCEPTED;
        this.resolvedAt = Instant.now();
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

    public RecipeSuggestionStatus getStatus() {
        return status;
    }

    public BigDecimal getScore() {
        return score;
    }

    public RejectionReason getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
