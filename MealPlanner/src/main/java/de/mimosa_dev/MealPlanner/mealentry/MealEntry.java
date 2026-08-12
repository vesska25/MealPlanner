package de.mimosa_dev.MealPlanner.mealentry;

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
 * FR-50/FR-50a: a logged meal occasion. INV-13: a {@code COOKED_DISH} entry always references a
 * finished dish + portions eaten; every other type never does — enforced here by construction
 * (see {@link #cookedDish} / {@link #other} factories) and again at the DB level (chk_meal_entry
 * _cooked_dish_consistency).
 */
@Entity
@Table(name = "meal_entry")
public class MealEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MealEntryType type;

    @Column(name = "cooked_dish_id")
    private Long cookedDishId;

    @Column(name = "portions_eaten")
    private BigDecimal portionsEaten;

    @Column(name = "kcal")
    private BigDecimal kcal;

    @Column(name = "protein_grams")
    private BigDecimal proteinGrams;

    @Column(name = "fat_grams")
    private BigDecimal fatGrams;

    @Column(name = "carbs_grams")
    private BigDecimal carbsGrams;

    @Column(name = "occurred_at", insertable = false, updatable = false)
    private Instant occurredAt;

    protected MealEntry() {
    }

    public static MealEntry cookedDish(Long userId, Long cookedDishId, BigDecimal portionsEaten) {
        MealEntry entry = new MealEntry();
        entry.userId = userId;
        entry.type = MealEntryType.COOKED_DISH;
        entry.cookedDishId = cookedDishId;
        entry.portionsEaten = portionsEaten;
        return entry;
    }

    public static MealEntry other(
            Long userId, MealEntryType type, BigDecimal kcal, BigDecimal proteinGrams, BigDecimal fatGrams, BigDecimal carbsGrams) {
        if (type == MealEntryType.COOKED_DISH) {
            throw new IllegalArgumentException("Use MealEntry.cookedDish(...) for COOKED_DISH entries");
        }
        MealEntry entry = new MealEntry();
        entry.userId = userId;
        entry.type = type;
        entry.kcal = kcal;
        entry.proteinGrams = proteinGrams;
        entry.fatGrams = fatGrams;
        entry.carbsGrams = carbsGrams;
        return entry;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public MealEntryType getType() {
        return type;
    }

    public Long getCookedDishId() {
        return cookedDishId;
    }

    public BigDecimal getPortionsEaten() {
        return portionsEaten;
    }

    public BigDecimal getKcal() {
        return kcal;
    }

    public BigDecimal getProteinGrams() {
        return proteinGrams;
    }

    public BigDecimal getFatGrams() {
        return fatGrams;
    }

    public BigDecimal getCarbsGrams() {
        return carbsGrams;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
