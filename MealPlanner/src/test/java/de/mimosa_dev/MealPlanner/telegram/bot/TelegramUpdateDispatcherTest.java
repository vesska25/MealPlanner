package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramCallbackQuery;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramChat;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramMessage;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramUpdate;
import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test, no Spring context: dispatch routing is pure logic (pick the first handler
 * whose {@code supports(...)} matches), so a real ApplicationContext buys nothing here.
 */
class TelegramUpdateDispatcherTest {

    private final TelegramCommandHandler startHandler = mock(TelegramCommandHandler.class);
    private final TelegramCommandHandler dayHandler = mock(TelegramCommandHandler.class);
    private final TelegramCallbackHandler cookHandler = mock(TelegramCallbackHandler.class);
    private final TelegramCallbackHandler dayStatusHandler = mock(TelegramCallbackHandler.class);

    private final TelegramUpdateDispatcher dispatcher = new TelegramUpdateDispatcher(
            List.of(startHandler, dayHandler), List.of(cookHandler, dayStatusHandler));

    @Test
    void routesATextMessageToTheFirstSupportingCommandHandler() {
        when(startHandler.supports("/start abc")).thenReturn(true);
        TelegramMessage message = new TelegramMessage(1, new TelegramChat(10), new TelegramUser(20, "u"), "/start abc");

        dispatcher.dispatch(new TelegramUpdate(1, message, null));

        verify(startHandler).handle(message);
        verify(dayHandler, never()).handle(any());
    }

    @Test
    void routesACallbackQueryToTheFirstSupportingCallbackHandler() {
        when(cookHandler.supports("cook:1:2")).thenReturn(true);
        TelegramCallbackQuery callback = new TelegramCallbackQuery("cbid", new TelegramUser(20, "u"), null, "cook:1:2");

        dispatcher.dispatch(new TelegramUpdate(1, null, callback));

        verify(cookHandler).handle(callback);
        verify(dayStatusHandler, never()).handle(any());
    }

    @Test
    void anUnrecognizedCommandIsSilentlyIgnored() {
        TelegramMessage message = new TelegramMessage(1, new TelegramChat(10), new TelegramUser(20, "u"), "/unknown");

        dispatcher.dispatch(new TelegramUpdate(1, message, null));

        verify(startHandler, never()).handle(any());
        verify(dayHandler, never()).handle(any());
    }
}
