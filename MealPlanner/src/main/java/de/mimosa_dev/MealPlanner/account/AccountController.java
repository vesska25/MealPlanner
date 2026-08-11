package de.mimosa_dev.MealPlanner.account;

import de.mimosa_dev.MealPlanner.account.dto.AccountExportResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/export")
    public AccountExportResponse export(@AuthenticationPrincipal Long userId) {
        return accountService.exportData(userId);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Long userId) {
        accountService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
