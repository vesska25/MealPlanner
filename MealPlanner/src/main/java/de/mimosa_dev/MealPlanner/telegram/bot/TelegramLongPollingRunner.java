package de.mimosa_dev.MealPlanner.telegram.bot;

import de.mimosa_dev.MealPlanner.telegram.bot.dto.TelegramUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Long polling (decided over webhooks: no public HTTPS endpoint exists in this project's infra).
 * {@code @ConditionalOnProperty} gates this bean's existence entirely — local dev without a
 * registered bot token just never creates it, same pattern as {@code anthropic.api-key} being
 * optional elsewhere in this codebase.
 *
 * <p>{@link SmartLifecycle} over a blocking {@code ApplicationRunner}: {@code start()} returns
 * immediately after spawning a daemon background thread (so it never blocks context startup or
 * prevents JVM exit), and {@code stop()} gives the container a real graceful-shutdown hook —
 * a bare {@code while(true)} in an ApplicationRunner has no clean way to signal shutdown.
 */
@Component
@ConditionalOnProperty(name = "telegram.bot-token")
public class TelegramLongPollingRunner implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelegramLongPollingRunner.class);
    private static final int POLL_TIMEOUT_SECONDS = 30;
    private static final long RETRY_BACKOFF_MILLIS = 5000;

    private final TelegramBotClient botClient;
    private final TelegramUpdateDispatcher dispatcher;

    private volatile boolean running = false;
    private volatile long offset = 0;
    private Thread pollingThread;

    public TelegramLongPollingRunner(TelegramBotClient botClient, TelegramUpdateDispatcher dispatcher) {
        this.botClient = botClient;
        this.dispatcher = dispatcher;
    }

    @Override
    public void start() {
        running = true;
        pollingThread = new Thread(this::pollLoop, "telegram-long-polling");
        pollingThread.setDaemon(true);
        pollingThread.start();
        log.info("Telegram long-polling runner started");
    }

    private void pollLoop() {
        while (running) {
            try {
                for (TelegramUpdate update : botClient.getUpdates(offset, POLL_TIMEOUT_SECONDS)) {
                    offset = update.updateId() + 1;
                    try {
                        dispatcher.dispatch(update);
                    } catch (Exception e) {
                        log.warn("Failed to dispatch Telegram update {}", update.updateId(), e);
                    }
                }
            } catch (Exception e) {
                log.warn("Telegram long-poll iteration failed, retrying in {}ms", RETRY_BACKOFF_MILLIS, e);
                sleepBackoff();
            }
        }
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
