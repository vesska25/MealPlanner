package de.mimosa_dev.MealPlanner.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A one-time registration code (FR-01, NFR-12). Stored in plaintext, unlike FR-80a's hashed
 * Telegram binding codes: an invite code isn't proving an existing identity the way a binding
 * code is — it's closer to a coupon — and the PRD only asks for hashing on the Telegram side.
 * Kept as a plain {@code Long} reference to the using account (not a JPA association), matching
 * the rest of the codebase's choice not to add object-graph navigation to {@code AppUser}.
 */
@Entity
@Table(name = "invite_code")
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "used_by_user_id")
    private Long usedByUserId;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected InviteCode() {
    }

    public InviteCode(String code) {
        this.code = code;
    }

    /**
     * Checks {@code usedAt}, not {@code usedByUserId}: the latter is nulled out (FK {@code ON
     * DELETE SET NULL}) if the account that used this code is later deleted (FR-05), and a code
     * must stay permanently used even then — otherwise deleting an account would silently
     * reopen it for reuse, undoing FR-01's one-time-use guarantee.
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(Long userId) {
        this.usedByUserId = userId;
        this.usedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Long getUsedByUserId() {
        return usedByUserId;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
