package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramUpdate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TelegramUpdateDispatcher {

    private final List<TelegramCommandHandler> commandHandlers;
    private final List<TelegramCallbackHandler> callbackHandlers;

    public TelegramUpdateDispatcher(List<TelegramCommandHandler> commandHandlers, List<TelegramCallbackHandler> callbackHandlers) {
        this.commandHandlers = commandHandlers;
        this.callbackHandlers = callbackHandlers;
    }

    public void dispatch(TelegramUpdate update) {
        if (update.message() != null && update.message().text() != null) {
            String text = update.message().text();
            commandHandlers.stream().filter(handler -> handler.supports(text)).findFirst()
                    .ifPresent(handler -> handler.handle(update.message()));
        } else if (update.callbackQuery() != null) {
            String data = update.callbackQuery().data();
            callbackHandlers.stream().filter(handler -> handler.supports(data)).findFirst()
                    .ifPresent(handler -> handler.handle(update.callbackQuery()));
        }
    }
}
