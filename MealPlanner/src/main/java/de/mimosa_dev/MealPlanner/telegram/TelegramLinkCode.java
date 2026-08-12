package de.mimosa_dev.MealPlanner.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A one-time Telegram-linking deep-link code (FR-80, 10-minute TTL). Unlike {@code InviteCode},
 * stored as a hash (FR-80a) — this code proves the identity of an already-authenticated web
 * session, not a coupon-style secret. Plain {@code Long userId} field, not a JPA association,
 * matching this codebase's established entity convention.
 */
@Entity
@Table(name = "telegram_link_code")
public class TelegramLinkCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected TelegramLinkCode() {
    }

    public TelegramLinkCode(Long userId, String codeHash, Instant expiresAt) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
