package de.mimosa_dev.MealPlanner.profile;

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
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The real user_profile entity (PRD section 6, FR-10 to FR-15, FR-70/71/71a/74) —
 * {@link de.mimosa_dev.MealPlanner.recipe.UserConstraints} has stood in for this since step 5.
 * Created by {@code FinalizeOnboardingTool} once every FR-11 mandatory field is collected.
 */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "household_size", nullable = false)
    private Integer householdSize;

    @Column(name = "max_cook_time_weekday_minutes", nullable = false)
    private Integer maxCookTimeWeekdayMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal")
    private Goal goal;

    @Column(name = "weekly_budget")
    private BigDecimal weeklyBudget;

    @Column(name = "preferred_stores")
    private String preferredStores;

    @Column(name = "country")
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex")
    private Sex sex;

    @Column(name = "age_years")
    private Integer ageYears;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    private ActivityLevel activityLevel;

    @Column(name = "goals_enabled", nullable = false)
    private boolean goalsEnabled;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "user_profile_excluded_product", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "product_id")
    private Set<Long> excludedProductIds = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "user_profile_equipment", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "equipment_name")
    private Set<String> equipment = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "user_profile_free_day", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> freeDays = new LinkedHashSet<>();

    protected UserProfile() {
    }

    public UserProfile(Long userId, Integer householdSize, Integer maxCookTimeWeekdayMinutes) {
        this.userId = userId;
        this.householdSize = householdSize;
        this.maxCookTimeWeekdayMinutes = maxCookTimeWeekdayMinutes;
        this.goalsEnabled = false;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getHouseholdSize() {
        return householdSize;
    }

    public void setHouseholdSize(Integer householdSize) {
        this.householdSize = householdSize;
    }

    public Integer getMaxCookTimeWeekdayMinutes() {
        return maxCookTimeWeekdayMinutes;
    }

    public void setMaxCookTimeWeekdayMinutes(Integer maxCookTimeWeekdayMinutes) {
        this.maxCookTimeWeekdayMinutes = maxCookTimeWeekdayMinutes;
    }

    public Goal getGoal() {
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    public BigDecimal getWeeklyBudget() {
        return weeklyBudget;
    }

    public void setWeeklyBudget(BigDecimal weeklyBudget) {
        this.weeklyBudget = weeklyBudget;
    }

    public String getPreferredStores() {
        return preferredStores;
    }

    public void setPreferredStores(String preferredStores) {
        this.preferredStores = preferredStores;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Integer getAgeYears() {
        return ageYears;
    }

    public void setAgeYears(Integer ageYears) {
        this.ageYears = ageYears;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public boolean isGoalsEnabled() {
        return goalsEnabled;
    }

    public void setGoalsEnabled(boolean goalsEnabled) {
        this.goalsEnabled = goalsEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Long> getExcludedProductIds() {
        return excludedProductIds;
    }

    public Set<String> getEquipment() {
        return equipment;
    }

    public Set<DayOfWeek> getFreeDays() {
        return freeDays;
    }
}
