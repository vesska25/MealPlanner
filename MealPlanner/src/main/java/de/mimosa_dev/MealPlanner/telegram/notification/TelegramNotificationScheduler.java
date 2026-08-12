package de.mimosa_dev.MealPlanner.telegram.notification;

import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListService;
import de.mimosa_dev.MealPlanner.telegram.TelegramLink;
import de.mimosa_dev.MealPlanner.telegram.TelegramLinkRepository;
import de.mimosa_dev.MealPlanner.telegram.bot.TelegramBotClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * FR-81 notification types 2 ("a product is about to spoil") and 3 ("a reminder to go
 * shopping") — periodic, unlike the event-driven new-suggestion notification, since neither has
 * a discrete domain event to hook into. The PRD states no concrete poll interval or spoilage
 * lead time anywhere; both are documented defaults (application.yml), not silently invented.
 */
@Component
@ConditionalOnProperty(name = "telegram.bot-token")
public class TelegramNotificationScheduler {

    private final TelegramLinkRepository telegramLinkRepository;
    private final PantryItemRepository pantryItemRepository;
    private final TelegramNotificationLogRepository notificationLogRepository;
    private final ShoppingListService shoppingListService;
    private final TelegramBotClient botClient;
    private final int spoilageLeadTimeDays;
    private final long shoppingReminderCooldownHours;

    public TelegramNotificationScheduler(
            TelegramLinkRepository telegramLinkRepository,
            PantryItemRepository pantryItemRepository,
            TelegramNotificationLogRepository notificationLogRepository,
            ShoppingListService shoppingListService,
            TelegramBotClient botClient,
            @Value("${telegram.spoilage-lead-time-days:1}") int spoilageLeadTimeDays,
            @Value("${telegram.shopping-reminder-cooldown-hours:24}") long shoppingReminderCooldownHours) {
        this.telegramLinkRepository = telegramLinkRepository;
        this.pantryItemRepository = pantryItemRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.shoppingListService = shoppingListService;
        this.botClient = botClient;
        this.spoilageLeadTimeDays = spoilageLeadTimeDays;
        this.shoppingReminderCooldownHours = shoppingReminderCooldownHours;
    }

    @Scheduled(fixedDelayString = "${telegram.notification-check-interval-ms:900000}")
    @Transactional
    public void checkAndNotify() {
        for (TelegramLink link : telegramLinkRepository.findAll()) {
            checkSpoilage(link);
            checkShoppingReminder(link);
        }
    }

    private void checkSpoilage(TelegramLink link) {
        LocalDate horizon = LocalDate.now().plusDays(spoilageLeadTimeDays);
        for (PantryItem item : pantryItemRepository.findByUserIdAndStatusOrderByExpiresAtAsc(link.getUserId(), PantryItemStatus.ACTIVE)) {
            if (item.getProduct().isStaple() || item.getExpiresAt().isAfter(horizon)) {
                continue;
            }
            if (notificationLogRepository.existsByUserIdAndTypeAndReferenceId(
                    link.getUserId(), TelegramNotificationType.SPOILAGE, item.getId())) {
                continue;
            }
            botClient.sendMessage(link.getTelegramUserId(),
                    "⚠️ " + item.getProduct().getCanonicalName() + " expires " + item.getExpiresAt(), null);
            notificationLogRepository.save(new TelegramNotificationLog(link.getUserId(), TelegramNotificationType.SPOILAGE, item.getId()));
        }
    }

    private void checkShoppingReminder(TelegramLink link) {
        if (!shoppingListService.isPantryRunningLow(link.getUserId())) {
            return;
        }
        var lastSent = notificationLogRepository.findTopByUserIdAndTypeOrderBySentAtDesc(
                link.getUserId(), TelegramNotificationType.SHOPPING_REMINDER);
        if (lastSent.isPresent() && lastSent.get().getSentAt().isAfter(
                Instant.now().minus(shoppingReminderCooldownHours, ChronoUnit.HOURS))) {
            return;
        }
        botClient.sendMessage(link.getTelegramUserId(), "🛒 Your pantry is running low — time to go shopping!", null);
        notificationLogRepository.save(new TelegramNotificationLog(link.getUserId(), TelegramNotificationType.SHOPPING_REMINDER, null));
    }
}
