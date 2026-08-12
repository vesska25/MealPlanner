package de.mimosa_dev.MealPlanner.telegram;

import de.mimosa_dev.MealPlanner.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TelegramLinkService.class)
@TestPropertySource(properties = "telegram.bot-username=meal_planner_test_bot")
class TelegramLinkServiceTest extends AbstractIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private TelegramLinkService telegramLinkService;

    @Autowired
    private TelegramLinkCodeRepository codeRepository;

    @Autowired
    private TelegramLinkRepository linkRepository;

    @BeforeEach
    void ensureUser() {
        ensureUserExists(USER_ID);
    }

    @Test
    void generatedCodeIsStoredHashedNotPlaintext() {
        var generated = telegramLinkService.generateLinkCode(USER_ID);

        TelegramLinkCode stored = codeRepository.findByUserIdAndUsedAtIsNull(USER_ID).getFirst();
        assertThat(stored.getCodeHash()).hasSize(64); // SHA-256 hex digest length
        assertThat(stored.getCodeHash()).isNotEqualTo(generated.code());
        assertThat(generated.deepLink()).isEqualTo("https://t.me/meal_planner_test_bot?start=" + generated.code());
    }

    @Test
    void consumingAValidCodeCreatesTheLink() {
        var generated = telegramLinkService.generateLinkCode(USER_ID);

        telegramLinkService.consumeLinkCode(generated.code(), 987654321L);

        assertThat(telegramLinkService.findUserIdByTelegramUserId(987654321L)).contains(USER_ID);
        assertThat(codeRepository.findByCodeHash(hashOf(generated.code())).orElseThrow().getUsedAt()).isNotNull();
    }

    @Test
    void reusingAnAlreadyConsumedCodeIsRejected() {
        var generated = telegramLinkService.generateLinkCode(USER_ID);
        telegramLinkService.consumeLinkCode(generated.code(), 111L);

        assertThatThrownBy(() -> telegramLinkService.consumeLinkCode(generated.code(), 222L))
                .isInstanceOf(InvalidLinkCodeException.class);
        // second attempt must not have replaced the first link
        assertThat(telegramLinkService.findUserIdByTelegramUserId(111L)).contains(USER_ID);
        assertThat(telegramLinkService.findUserIdByTelegramUserId(222L)).isEmpty();
    }

    @Test
    void anExpiredCodeIsRejected() {
        codeRepository.save(new TelegramLinkCode(USER_ID, hashOf("expired-code"), Instant.now().minusSeconds(1)));

        assertThatThrownBy(() -> telegramLinkService.consumeLinkCode("expired-code", 333L))
                .isInstanceOf(InvalidLinkCodeException.class);
    }

    @Test
    void generatingANewCodeInvalidatesAnyPriorUnusedCode() {
        var first = telegramLinkService.generateLinkCode(USER_ID);
        telegramLinkService.generateLinkCode(USER_ID);

        assertThatThrownBy(() -> telegramLinkService.consumeLinkCode(first.code(), 444L))
                .isInstanceOf(InvalidLinkCodeException.class);
    }

    @Test
    void unlinkRemovesTheLink() {
        var generated = telegramLinkService.generateLinkCode(USER_ID);
        telegramLinkService.consumeLinkCode(generated.code(), 555L);

        telegramLinkService.unlink(USER_ID);

        assertThat(telegramLinkService.findLinkByUserId(USER_ID)).isEmpty();
    }

    private static String hashOf(String rawCode) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(rawCode.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
