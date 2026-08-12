package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.telegram.bot.TelegramLongPollingRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for a real bug found manually: giving `telegram.bot-token` a blanket
 * `${TELEGRAM_BOT_TOKEN:}` default in application.yml made the property ALWAYS present (as an
 * empty string), so `@ConditionalOnProperty(name = "telegram.bot-token")` — which only checks
 * for the property's absence, not blankness — was always true, starting the long-polling runner
 * even with no bot configured at all. Fixed by removing that blanket default so the property
 * genuinely doesn't exist unless TELEGRAM_BOT_TOKEN is actually set.
 */
class TelegramConditionalBeanTest extends AbstractApiIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void theLongPollingRunnerDoesNotExistWithoutABotTokenConfigured() {
        assertThat(applicationContext.getBeanNamesForType(TelegramLongPollingRunner.class)).isEmpty();
    }
}
