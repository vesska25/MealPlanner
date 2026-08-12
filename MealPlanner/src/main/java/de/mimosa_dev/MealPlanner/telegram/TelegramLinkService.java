package de.mimosa_dev.MealPlanner.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * FR-80/80a/80b/84: Telegram account linking. The linking code is a high-entropy random token
 * (not a low-entropy user-chosen secret like a password), so it's hashed with SHA-256 rather than
 * BCrypt — a fast, deterministic hash supports an indexed {@code WHERE code_hash = ?} lookup,
 * which BCrypt's non-deterministic salting cannot; BCrypt's slow-hash defense exists specifically
 * to slow down brute-forcing of low-entropy secrets, which doesn't apply here.
 */
@Service
public class TelegramLinkService {

    private static final int CODE_BYTES = 24;
    private static final Duration CODE_TTL = Duration.ofMinutes(10);

    private final TelegramLinkCodeRepository codeRepository;
    private final TelegramLinkRepository linkRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String botUsername;

    public TelegramLinkService(
            TelegramLinkCodeRepository codeRepository,
            TelegramLinkRepository linkRepository,
            @Value("${telegram.bot-username:}") String botUsername) {
        this.codeRepository = codeRepository;
        this.linkRepository = linkRepository;
        this.botUsername = botUsername;
    }

    public record GeneratedLinkCode(String code, String deepLink, Instant expiresAt) {
    }

    /**
     * FR-80: a fresh 10-minute deep-link code. Mirrors RecipeSuggestionService.activate()'s
     * "expire whatever was previously active, then create the new one" pattern — a stale,
     * still-unexpired code from an earlier click of "Connect Telegram" is invalidated rather
     * than left dangling and independently usable.
     */
    @Transactional
    public GeneratedLinkCode generateLinkCode(Long userId) {
        codeRepository.findByUserIdAndUsedAtIsNull(userId).forEach(TelegramLinkCode::markUsed);

        byte[] raw = new byte[CODE_BYTES];
        secureRandom.nextBytes(raw);
        String rawCode = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant expiresAt = Instant.now().plus(CODE_TTL);
        codeRepository.save(new TelegramLinkCode(userId, sha256Hex(rawCode), expiresAt));

        return new GeneratedLinkCode(rawCode, "https://t.me/" + botUsername + "?start=" + rawCode, expiresAt);
    }

    /**
     * The public entry point of the linking flow — there's no authenticated userId at this
     * point, identity comes FROM this call (same category as registration).
     */
    @Transactional
    public void consumeLinkCode(String rawCode, Long telegramUserId) {
        TelegramLinkCode code = codeRepository.findByCodeHash(sha256Hex(rawCode))
                .filter(TelegramLinkCode::isValid)
                .orElseThrow(InvalidLinkCodeException::new);
        code.markUsed(); // FR-80a: invalidated after successful linking, cannot be reused

        linkRepository.findByTelegramUserId(telegramUserId).ifPresent(linkRepository::delete);
        linkRepository.findByUserId(code.getUserId()).ifPresent(linkRepository::delete);
        linkRepository.save(new TelegramLink(code.getUserId(), telegramUserId));
    }

    @Transactional(readOnly = true)
    public Optional<Long> findUserIdByTelegramUserId(Long telegramUserId) {
        return linkRepository.findByTelegramUserId(telegramUserId).map(TelegramLink::getUserId);
    }

    @Transactional(readOnly = true)
    public Optional<TelegramLink> findLinkByUserId(Long userId) {
        return linkRepository.findByUserId(userId);
    }

    /** FR-84: unlinking is web-only — no bot-side entry point calls this. */
    @Transactional
    public void unlink(Long userId) {
        TelegramLink link = linkRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("No Telegram link for user " + userId));
        linkRepository.delete(link);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
