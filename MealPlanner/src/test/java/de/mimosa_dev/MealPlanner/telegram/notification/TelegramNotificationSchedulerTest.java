package de.mimosa_dev.MealPlanner.telegram.notification;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductNormalizationService;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListService;
import de.mimosa_dev.MealPlanner.telegram.TelegramLinkRepository;
import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;
import de.mimosa_dev.MealPlanner.telegram.bot.TelegramBotClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({
        TelegramNotificationScheduler.class, ShoppingListService.class, RecipeSuggestionService.class,
        PantryService.class, ProductNormalizationService.class, TelegramLinkService.class
})
// TelegramNotificationScheduler is @ConditionalOnProperty(name = "telegram.bot-token") — that
// condition evaluates regardless of @Import, so the property needs a value here too.
@TestPropertySource(properties = "telegram.bot-token=test-token")
class TelegramNotificationSchedulerTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long TELEGRAM_USER_ID = 999L;

    @Autowired
    private TelegramNotificationScheduler scheduler;

    @Autowired
    private TelegramLinkRepository telegramLinkRepository;

    @Autowired
    private TelegramNotificationLogRepository notificationLogRepository;

    @Autowired
    private PantryItemRepository pantryItemRepository;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private TelegramBotClient botClient;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        ensureUserExists(USER_ID);
        telegramLinkRepository.save(new de.mimosa_dev.MealPlanner.telegram.TelegramLink(USER_ID, TELEGRAM_USER_ID));
    }

    @Test
    void notifiesOnceForAnItemExpiringWithinTheLeadTimeThenNotAgain() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        PantryItem item = pantryService.addStock(USER_ID, milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());
        item.setExpiresAt(LocalDate.now().plusDays(1)); // within the default 1-day lead time
        pantryItemRepository.save(item);
        // A second, well-stocked item with plenty of shelf life left so the pantry doesn't ALSO
        // read as "running low" (ShoppingListService.isPantryRunningLow) — isolates this test to
        // the spoilage trigger only, since both are legitimately independent conditions.
        Product rice = productRepository.findByCanonicalNameIgnoreCase("rice").orElseThrow();
        PantryItem wellStocked = pantryService.addStock(USER_ID, rice, new BigDecimal("2000"), Unit.GRAM, LocalDate.now());
        wellStocked.setExpiresAt(LocalDate.now().plusDays(60));
        pantryItemRepository.save(wellStocked);

        scheduler.checkAndNotify();
        // Simulates two separate @Scheduled firings (each a genuinely fresh transaction in
        // production) within this one test transaction — without flush+clear, the just-saved
        // TelegramNotificationLog stays in the persistence-context identity map with its
        // insertable=false sentAt still null in memory, rather than being re-read from Postgres.
        entityManager.flush();
        entityManager.clear();
        scheduler.checkAndNotify(); // second run must not re-notify for the same item

        verify(botClient, times(1)).sendMessage(anyLong(), any(), any());
        assertThat(notificationLogRepository.existsByUserIdAndTypeAndReferenceId(
                USER_ID, TelegramNotificationType.SPOILAGE, item.getId())).isTrue();
    }

    @Test
    void doesNotNotifyForAnItemExpiringWellBeyondTheLeadTime() {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        PantryItem item = pantryService.addStock(USER_ID, milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());
        item.setExpiresAt(LocalDate.now().plusDays(30));
        pantryItemRepository.save(item);

        scheduler.checkAndNotify();

        verify(botClient, times(0)).sendMessage(anyLong(), any(), any());
    }

    @Test
    void notifiesForAnEmptyPantryAsRunningLowThenRespectsTheCooldown() {
        // No pantry stock at all for USER_ID — ShoppingListService.isPantryRunningLow treats an
        // empty active/non-staple list as "running low".
        scheduler.checkAndNotify();
        entityManager.flush();
        entityManager.clear();
        scheduler.checkAndNotify(); // second run within the cooldown window must not re-notify

        verify(botClient, times(1)).sendMessage(anyLong(), any(), any());
        assertThat(notificationLogRepository.findTopByUserIdAndTypeOrderBySentAtDesc(
                USER_ID, TelegramNotificationType.SHOPPING_REMINDER)).isPresent();
    }
}
