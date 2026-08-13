package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.profile.UserProfile;
import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import de.mimosa_dev.MealPlanner.profile.onboarding.OnboardingDraftService;
import de.mimosa_dev.MealPlanner.profile.onboarding.dto.OnboardingStateResponse;
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

class OnboardingControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OnboardingDraftService onboardingDraftService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void reportsUnfinalizedWithNoTurnsForABrandNewUser() {
        Registration registration = register();

        ResponseEntity<OnboardingStateResponse> response = exchangeAuthenticated(registration.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().profileFinalized()).isFalse();
        assertThat(response.getBody().recentTurns()).isEmpty();
    }

    @Test
    void reflectsSavedTurnsForAnInProgressOnboarding() {
        Registration registration = register();
        onboardingDraftService.appendTurn(registration.userId(), "user", "I cook for 2 people");

        ResponseEntity<OnboardingStateResponse> response = exchangeAuthenticated(registration.token());

        assertThat(response.getBody().recentTurns()).hasSize(1);
        assertThat(response.getBody().recentTurns().get(0).text()).isEqualTo("I cook for 2 people");
    }

    @Test
    void reportsFinalizedOnceTheRealProfileExists() {
        Registration registration = register();
        userProfileRepository.save(new UserProfile(registration.userId(), 2, 30));

        ResponseEntity<OnboardingStateResponse> response = exchangeAuthenticated(registration.token());

        assertThat(response.getBody().profileFinalized()).isTrue();
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange("/api/onboarding/state", HttpMethod.GET, HttpEntity.EMPTY, String.class);

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

    private ResponseEntity<OnboardingStateResponse> exchangeAuthenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/api/onboarding/state", HttpMethod.GET, new HttpEntity<>(headers), OnboardingStateResponse.class);
    }
}
