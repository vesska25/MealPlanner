package de.mimosa_dev.MealPlanner.telegram.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Dedup record for the two polled notification types (FR-81 types 2/3) — see {@link TelegramNotificationLogRepository}. */
@Entity
@Table(name = "telegram_notification_log")
public class TelegramNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TelegramNotificationType type;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "sent_at", insertable = false, updatable = false)
    private Instant sentAt;

    protected TelegramNotificationLog() {
    }

    public TelegramNotificationLog(Long userId, TelegramNotificationType type, Long referenceId) {
        this.userId = userId;
        this.type = type;
        this.referenceId = referenceId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public TelegramNotificationType getType() {
        return type;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
