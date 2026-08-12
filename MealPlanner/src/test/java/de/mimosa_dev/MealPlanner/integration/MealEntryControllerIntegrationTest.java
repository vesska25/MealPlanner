package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.mealentry.MealEntryType;
import de.mimosa_dev.MealPlanner.mealentry.dto.CreateMealEntryRequest;
import de.mimosa_dev.MealPlanner.mealentry.dto.MealEntryResponse;
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

class MealEntryControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void recordingANonCookedStatusSucceeds() {
        Registration registration = register();

        ResponseEntity<MealEntryResponse> response = exchangeAuthenticated(
                registration.token(), new CreateMealEntryRequest(MealEntryType.OUTSIDE, null, null, null, null),
                MealEntryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().type()).isEqualTo(MealEntryType.OUTSIDE);
        assertThat(response.getBody().cookedDishId()).isNull();
    }

    @Test
    void recordingCookedDishDirectlyIsRejected() {
        Registration registration = register();

        ResponseEntity<String> response = exchangeAuthenticated(
                registration.token(), new CreateMealEntryRequest(MealEntryType.COOKED_DISH, null, null, null, null),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/meal-entries", HttpMethod.POST, HttpEntity.EMPTY, String.class);

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

    private <T> ResponseEntity<T> exchangeAuthenticated(String token, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/meal-entries", HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }
}
