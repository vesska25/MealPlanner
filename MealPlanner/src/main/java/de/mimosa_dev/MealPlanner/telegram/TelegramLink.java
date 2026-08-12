package de.mimosa_dev.MealPlanner.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A confirmed link between an app account and a Telegram account (FR-80b: by numeric Telegram
 * user id, never username). One row per side, enforced by unique constraints in the migration.
 */
@Entity
@Table(name = "telegram_link")
public class TelegramLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Column(name = "linked_at", insertable = false, updatable = false)
    private Instant linkedAt;

    protected TelegramLink() {
    }

    public TelegramLink(Long userId, Long telegramUserId) {
        this.userId = userId;
        this.telegramUserId = telegramUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
