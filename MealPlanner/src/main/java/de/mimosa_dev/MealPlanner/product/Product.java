package de.mimosa_dev.MealPlanner.product;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Product reference entry (section 6). Canonical name is the cross-system key;
 * nutrition fields are nullable because unverified, free-text-created entries
 * can lack them and simply won't be used in calculations (FR-43b).
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_name", nullable = false, unique = true)
    private String canonicalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Column(name = "kcal_per_100g")
    private BigDecimal kcalPer100g;

    @Column(name = "protein_per_100g")
    private BigDecimal proteinPer100g;

    @Column(name = "fat_per_100g")
    private BigDecimal fatPer100g;

    @Column(name = "carbs_per_100g")
    private BigDecimal carbsPer100g;

    @Column(name = "default_shelf_life_days", nullable = false)
    private Integer defaultShelfLifeDays;

    @Column(name = "grams_per_piece")
    private BigDecimal gramsPerPiece;

    @Column(name = "is_staple", nullable = false)
    private boolean staple;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "product_synonym", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "synonym")
    private Set<String> synonyms = new LinkedHashSet<>();

    protected Product() {
    }

    public Long getId() {
        return id;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public BigDecimal getKcalPer100g() {
        return kcalPer100g;
    }

    public void setKcalPer100g(BigDecimal kcalPer100g) {
        this.kcalPer100g = kcalPer100g;
    }

    public BigDecimal getProteinPer100g() {
        return proteinPer100g;
    }

    public void setProteinPer100g(BigDecimal proteinPer100g) {
        this.proteinPer100g = proteinPer100g;
    }

    public BigDecimal getFatPer100g() {
        return fatPer100g;
    }

    public void setFatPer100g(BigDecimal fatPer100g) {
        this.fatPer100g = fatPer100g;
    }

    public BigDecimal getCarbsPer100g() {
        return carbsPer100g;
    }

    public void setCarbsPer100g(BigDecimal carbsPer100g) {
        this.carbsPer100g = carbsPer100g;
    }

    public Integer getDefaultShelfLifeDays() {
        return defaultShelfLifeDays;
    }

    public void setDefaultShelfLifeDays(Integer defaultShelfLifeDays) {
        this.defaultShelfLifeDays = defaultShelfLifeDays;
    }

    public BigDecimal getGramsPerPiece() {
        return gramsPerPiece;
    }

    public void setGramsPerPiece(BigDecimal gramsPerPiece) {
        this.gramsPerPiece = gramsPerPiece;
    }

    public boolean isStaple() {
        return staple;
    }

    public void setStaple(boolean staple) {
        this.staple = staple;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<String> getSynonyms() {
        return synonyms;
    }
}
