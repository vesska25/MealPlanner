package de.mimosa_dev.MealPlanner.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Categorical shelf-life reference for cooked dishes (FR-52a), seed-migrated like
 * {@code product}'s own defaults rather than computed or model-generated.
 */
@Entity
@Table(name = "dish_category_shelf_life")
public class DishCategoryShelfLife {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private DishCategory category;

    @Column(name = "shelf_life_days", nullable = false)
    private Integer shelfLifeDays;

    protected DishCategoryShelfLife() {
    }

    public DishCategory getCategory() {
        return category;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }
}
