package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.telegram.dto.GenerateLinkCodeResponse;
import de.mimosa_dev.MealPlanner.telegram.dto.LinkTelegramRequest;
import de.mimosa_dev.MealPlanner.telegram.dto.TelegramLinkStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void fullLinkingLifecycle() {
        Registration registration = register();

        ResponseEntity<GenerateLinkCodeResponse> codeResponse = exchangeAuthenticated(
                registration.token(), "/api/telegram/link-code", HttpMethod.POST, null, GenerateLinkCodeResponse.class);
        assertThat(codeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(codeResponse.getBody().code()).isNotBlank();
        assertThat(codeResponse.getBody().deepLink()).contains(codeResponse.getBody().code());

        // The public /link endpoint must work with NO Authorization header at all — this is the
        // whole point of it being public (identity is established BY this call).
        ResponseEntity<Void> linkResponse = restTemplate.postForEntity(
                "/api/telegram/link", new LinkTelegramRequest(codeResponse.getBody().code(), 123456789L), Void.class);
        assertThat(linkResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<TelegramLinkStatusResponse> statusResponse = exchangeAuthenticated(
                registration.token(), "/api/telegram/status", HttpMethod.GET, null, TelegramLinkStatusResponse.class);
        assertThat(statusResponse.getBody().linked()).isTrue();
        assertThat(statusResponse.getBody().telegramUserId()).isEqualTo(123456789L);

        // FR-80a: the same code cannot be reused.
        ResponseEntity<String> reuseResponse = restTemplate.postForEntity(
                "/api/telegram/link", new LinkTelegramRequest(codeResponse.getBody().code(), 987654321L), String.class);
        assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // FR-84: unlink is web-only, authenticated.
        ResponseEntity<Void> unlinkResponse = exchangeAuthenticated(
                registration.token(), "/api/telegram", HttpMethod.DELETE, null, Void.class);
        assertThat(unlinkResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<TelegramLinkStatusResponse> afterUnlink = exchangeAuthenticated(
                registration.token(), "/api/telegram/status", HttpMethod.GET, null, TelegramLinkStatusResponse.class);
        assertThat(afterUnlink.getBody().linked()).isFalse();
    }

    @Test
    void linkingWithAnInvalidCodeIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/telegram/link", new LinkTelegramRequest("not-a-real-code", 1L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void generatingALinkCodeWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/telegram/link-code", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private record Registration(Long userId, String token) {
    }

    private Registration register() {
        String code = "TEST-" + UUID.randomUUID();
        inviteCodeRepository.save(new InviteCode(code));
        String email = "user-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "password123", code), AuthResponse.class);
        Long userId = appUserRepository.findByEmail(email).orElseThrow().getId();
        return new Registration(userId, response.getBody().token());
    }

    private <T> ResponseEntity<T> exchangeAuthenticated(
            String token, String path, HttpMethod method, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), responseType);
    }
}
