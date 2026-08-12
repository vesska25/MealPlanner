package de.mimosa_dev.MealPlanner.telegram.controller;

import de.mimosa_dev.MealPlanner.telegram.TelegramLinkService;
import de.mimosa_dev.MealPlanner.telegram.dto.GenerateLinkCodeResponse;
import de.mimosa_dev.MealPlanner.telegram.dto.LinkTelegramRequest;
import de.mimosa_dev.MealPlanner.telegram.dto.TelegramLinkStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-80/84: linking-code generation, unlinking, and status are authenticated (web-only actions).
 * {@code POST /link} is deliberately the one public endpoint here — see {@code SecurityConfig} —
 * since consuming a code is how identity gets established in the first place, same category as
 * {@code /api/auth/register}.
 */
@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private final TelegramLinkService telegramLinkService;

    public TelegramController(TelegramLinkService telegramLinkService) {
        this.telegramLinkService = telegramLinkService;
    }

    @PostMapping("/link-code")
    public GenerateLinkCodeResponse generateLinkCode(@AuthenticationPrincipal Long userId) {
        return GenerateLinkCodeResponse.from(telegramLinkService.generateLinkCode(userId));
    }

    @PostMapping("/link")
    public ResponseEntity<Void> link(@Valid @RequestBody LinkTelegramRequest request) {
        telegramLinkService.consumeLinkCode(request.code(), request.telegramUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public TelegramLinkStatusResponse status(@AuthenticationPrincipal Long userId) {
        return telegramLinkService.findLinkByUserId(userId)
                .map(TelegramLinkStatusResponse::from)
                .orElseGet(TelegramLinkStatusResponse::notLinked);
    }

    @DeleteMapping
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal Long userId) {
        telegramLinkService.unlink(userId);
        return ResponseEntity.noContent().build();
    }
}
