package de.mimosa_dev.MealPlanner.telegram.notification;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramNotificationLogRepositoryTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private TelegramNotificationLogRepository repository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void existsByUserIdAndTypeAndReferenceIdFindsAMatchingRow() {
        repository.save(new TelegramNotificationLog(USER_ID, TelegramNotificationType.SPOILAGE, 42L));

        assertThat(repository.existsByUserIdAndTypeAndReferenceId(USER_ID, TelegramNotificationType.SPOILAGE, 42L)).isTrue();
        assertThat(repository.existsByUserIdAndTypeAndReferenceId(USER_ID, TelegramNotificationType.SPOILAGE, 99L)).isFalse();
        assertThat(repository.existsByUserIdAndTypeAndReferenceId(USER_ID, TelegramNotificationType.SHOPPING_REMINDER, 42L)).isFalse();
    }

    @Test
    void findTopByUserIdAndTypeOrderBySentAtDescReturnsTheMostRecentRow() throws InterruptedException {
        repository.save(new TelegramNotificationLog(USER_ID, TelegramNotificationType.SHOPPING_REMINDER, null));
        Thread.sleep(10); // TIMESTAMPTZ has microsecond precision but DEFAULT now() calls can tie at millisecond granularity
        TelegramNotificationLog second = repository.save(new TelegramNotificationLog(USER_ID, TelegramNotificationType.SHOPPING_REMINDER, null));

        var mostRecent = repository.findTopByUserIdAndTypeOrderBySentAtDesc(USER_ID, TelegramNotificationType.SHOPPING_REMINDER);

        assertThat(mostRecent).isPresent();
        assertThat(mostRecent.get().getId()).isEqualTo(second.getId());
    }

    @Test
    void findTopReturnsEmptyWhenNothingHasBeenSent() {
        assertThat(repository.findTopByUserIdAndTypeOrderBySentAtDesc(USER_ID, TelegramNotificationType.NEW_SUGGESTION)).isEmpty();
    }
}
