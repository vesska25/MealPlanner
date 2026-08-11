package de.mimosa_dev.MealPlanner.recipe;

import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
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

import java.math.BigDecimal;

/**
 * A persisted line of {@link Recipe#getIngredients()}. Named with the {@code Entity} suffix to
 * avoid colliding with {@link RecipeIngredient}, the in-memory validation-layer DTO of the same
 * concept (PRD step 5) — the two aren't interchangeable: this one is a JPA-managed row with a
 * {@code Recipe} owner, that one is a plain record passed into {@link RecipeValidator}.
 */
@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private Unit unit;

    protected RecipeIngredientEntity() {
    }

    public RecipeIngredientEntity(Product product, BigDecimal quantity, Unit unit) {
        this.product = product;
        this.quantity = quantity;
        this.unit = unit;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Unit getUnit() {
        return unit;
    }
}
