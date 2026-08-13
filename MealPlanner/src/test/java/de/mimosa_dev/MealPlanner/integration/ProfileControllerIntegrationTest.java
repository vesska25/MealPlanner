package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.profile.ActivityLevel;
import de.mimosa_dev.MealPlanner.profile.Sex;
import de.mimosa_dev.MealPlanner.profile.UserProfile;
import de.mimosa_dev.MealPlanner.profile.UserProfileRepository;
import de.mimosa_dev.MealPlanner.profile.dto.GoalsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void returnsNotFoundWhenOnboardingIsIncomplete() {
        Registration registration = register();

        ResponseEntity<String> response = exchangeAuthenticated(registration.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenGoalsAreDisabled() {
        Registration registration = register();
        userProfileRepository.save(new UserProfile(registration.userId(), 2, 30)); // goalsEnabled defaults false

        ResponseEntity<String> response = exchangeAuthenticated(registration.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsComputedTargetsWhenGoalsAreEnabled() {
        Registration registration = register();
        UserProfile profile = new UserProfile(registration.userId(), 2, 30);
        profile.setSex(Sex.MALE);
        profile.setAgeYears(30);
        profile.setHeightCm(new BigDecimal("180"));
        profile.setWeightKg(new BigDecimal("80"));
        profile.setActivityLevel(ActivityLevel.SEDENTARY);
        profile.setGoalsEnabled(true);
        userProfileRepository.save(profile);

        ResponseEntity<GoalsResponse> response = restTemplate.exchange(
                "/api/profile/goals", HttpMethod.GET, authenticatedRequest(registration.token()), GoalsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().dailyKcal()).isEqualByComparingTo("2136.00");
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange("/api/profile/goals", HttpMethod.GET, HttpEntity.EMPTY, String.class);

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

    private ResponseEntity<String> exchangeAuthenticated(String token) {
        return restTemplate.exchange("/api/profile/goals", HttpMethod.GET, authenticatedRequest(token), String.class);
    }

    private HttpEntity<Void> authenticatedRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
