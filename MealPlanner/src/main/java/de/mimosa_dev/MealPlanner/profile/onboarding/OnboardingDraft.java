package de.mimosa_dev.MealPlanner.profile.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * FR-10a/FR-10b: the in-progress onboarding profile plus the last few dialogue turns, one row
 * per user. Both JSON columns are plain {@code TEXT} — see V16's migration comment for why not
 * {@code jsonb}. Deleted by {@link OnboardingDraftService#finalizeProfile} once the real
 * {@link de.mimosa_dev.MealPlanner.profile.UserProfile} is created.
 */
@Entity
@Table(name = "onboarding_draft")
public class OnboardingDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "draft_json", nullable = false)
    private String draftJson;

    @Column(name = "recent_turns_json", nullable = false)
    private String recentTurnsJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected OnboardingDraft() {
    }

    public OnboardingDraft(Long userId) {
        this.userId = userId;
        this.draftJson = "{}";
        this.recentTurnsJson = "[]";
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDraftJson() {
        return draftJson;
    }

    public void setDraftJson(String draftJson) {
        this.draftJson = draftJson;
    }

    public String getRecentTurnsJson() {
        return recentTurnsJson;
    }

    public void setRecentTurnsJson(String recentTurnsJson) {
        this.recentTurnsJson = recentTurnsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
