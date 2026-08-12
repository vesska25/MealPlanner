package de.mimosa_dev.MealPlanner.telegram.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramNotificationLogRepository extends JpaRepository<TelegramNotificationLog, Long> {

    // SPOILAGE dedup: one-shot per (user, item) — once notified, never again for that item.
    boolean existsByUserIdAndTypeAndReferenceId(Long userId, TelegramNotificationType type, Long referenceId);

    // SHOPPING_REMINDER dedup: no natural reference id, so a cooldown against the most recent
    // send is used instead — see TelegramNotificationScheduler.
    Optional<TelegramNotificationLog> findTopByUserIdAndTypeOrderBySentAtDesc(Long userId, TelegramNotificationType type);
}
