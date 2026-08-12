package de.mimosa_dev.MealPlanner.telegram.notification;

import de.mimosa_dev.MealPlanner.recipe.SuggestionActivatedEvent;
import de.mimosa_dev.MealPlanner.telegram.TelegramLink;
import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;
import de.mimosa_dev.MealPlanner.telegram.bot.TelegramBotClient;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.InlineKeyboardMarkup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Plain unit test, no Spring context: real @TransactionalEventListener/AFTER_COMMIT wiring is a framework guarantee, not re-tested here. */
class SuggestionActivatedEventListenerTest {

    private final TelegramLinkService telegramLinkService = mock(TelegramLinkService.class);
    private final TelegramBotClient botClient = mock(TelegramBotClient.class);
    private final SuggestionActivatedEventListener listener =
            new SuggestionActivatedEventListener(telegramLinkService, botClient);

    @Test
    void sendsANotificationWithACookConfirmButtonWhenTheUserHasALinkedTelegramAccount() {
        TelegramLink link = new TelegramLink(1L, 555L);
        when(telegramLinkService.findLinkByUserId(1L)).thenReturn(Optional.of(link));

        listener.onSuggestionActivated(new SuggestionActivatedEvent(1L, 42L, "Milk soup", 2, new BigDecimal("0.6")));

        verify(botClient).sendMessage(eq(555L), eq("🍳 New suggestion: Milk soup"), any(InlineKeyboardMarkup.class));
    }

    @Test
    void doesNothingWhenTheUserHasNoLinkedTelegramAccount() {
        when(telegramLinkService.findLinkByUserId(1L)).thenReturn(Optional.empty());

        listener.onSuggestionActivated(new SuggestionActivatedEvent(1L, 42L, "Milk soup", 2, new BigDecimal("0.6")));

        verify(botClient, never()).sendMessage(anyLong(), any(), any());
    }
}
