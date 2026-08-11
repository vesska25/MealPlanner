package de.mimosa_dev.MealPlanner.cooking;

import de.mimosa_dev.MealPlanner.recipe.DishCategory;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Cooked portions from a single confirmed cooking event (FR-52). A separate inventory from
 * {@code pantry_item} (FR-52b), with the same kind of lifecycle: created once, then only its
 * {@code portionsRemaining}/{@code status} change — never deleted, so history survives.
 * {@code kcal/protein/fat/carbsPerPortion} are frozen at creation (FR-54/INV-05): later changes
 * to the product catalogue never retroactively alter an already-cooked dish's numbers.
 */
@Entity
@Table(name = "cooked_dish")
public class CookedDish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private DishCategory category;

    @Column(name = "total_portions", nullable = false)
    private BigDecimal totalPortions;

    @Column(name = "portions_remaining", nullable = false)
    private BigDecimal portionsRemaining;

    @Column(name = "kcal_per_portion")
    private BigDecimal kcalPerPortion;

    @Column(name = "protein_per_portion")
    private BigDecimal proteinPerPortion;

    @Column(name = "fat_per_portion")
    private BigDecimal fatPerPortion;

    @Column(name = "carbs_per_portion")
    private BigDecimal carbsPerPortion;

    @Column(name = "cooked_at", nullable = false)
    private LocalDate cookedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CookedDishStatus status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected CookedDish() {
    }

    public CookedDish(
            Long userId, Recipe recipe, DishCategory category, BigDecimal totalPortions,
            BigDecimal kcalPerPortion, BigDecimal proteinPerPortion, BigDecimal fatPerPortion, BigDecimal carbsPerPortion,
            LocalDate cookedAt, LocalDate expiresAt, String idempotencyKey) {
        this.userId = userId;
        this.recipe = recipe;
        this.category = category;
        this.totalPortions = totalPortions;
        this.portionsRemaining = totalPortions;
        this.kcalPerPortion = kcalPerPortion;
        this.proteinPerPortion = proteinPerPortion;
        this.fatPerPortion = fatPerPortion;
        this.carbsPerPortion = carbsPerPortion;
        this.cookedAt = cookedAt;
        this.expiresAt = expiresAt;
        this.status = CookedDishStatus.ACTIVE;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public DishCategory getCategory() {
        return category;
    }

    public BigDecimal getTotalPortions() {
        return totalPortions;
    }

    public BigDecimal getPortionsRemaining() {
        return portionsRemaining;
    }

    public void setPortionsRemaining(BigDecimal portionsRemaining) {
        this.portionsRemaining = portionsRemaining;
    }

    public BigDecimal getKcalPerPortion() {
        return kcalPerPortion;
    }

    public BigDecimal getProteinPerPortion() {
        return proteinPerPortion;
    }

    public BigDecimal getFatPerPortion() {
        return fatPerPortion;
    }

    public BigDecimal getCarbsPerPortion() {
        return carbsPerPortion;
    }

    public LocalDate getCookedAt() {
        return cookedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public CookedDishStatus getStatus() {
        return status;
    }

    public void setStatus(CookedDishStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
