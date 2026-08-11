package de.mimosa_dev.MealPlanner.recipe;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A recipe that passed {@link RecipeValidator} (PRD 9.1 step 7, FR-34). Only validated
 * candidates ever become a row here — a recipe failing hard-constraint validation is
 * discarded upstream, never persisted (AI-08).
 */
@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "cook_time_minutes", nullable = false)
    private Integer cookTimeMinutes;

    @Column(name = "base_portions", nullable = false)
    private Integer basePortions;

    @ElementCollection
    @CollectionTable(name = "recipe_equipment", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "equipment")
    private Set<String> requiredEquipment = new LinkedHashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredientEntity> ingredients = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Recipe() {
    }

    public Recipe(Long userId, String name, Integer cookTimeMinutes, Integer basePortions, Set<String> requiredEquipment) {
        this.userId = userId;
        this.name = name;
        this.cookTimeMinutes = cookTimeMinutes;
        this.basePortions = basePortions;
        this.requiredEquipment = new LinkedHashSet<>(requiredEquipment);
    }

    public void addIngredient(RecipeIngredientEntity ingredient) {
        ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Integer getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public Integer getBasePortions() {
        return basePortions;
    }

    public Set<String> getRequiredEquipment() {
        return requiredEquipment;
    }

    public List<RecipeIngredientEntity> getIngredients() {
        return ingredients;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
