package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.account.dto.AccountExportResponse;
import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.LoginRequest;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The first true end-to-end tests in this project: real HTTP requests through the actual
 * security filter chain (register -> login -> authenticated access -> export -> delete),
 * against a live Postgres via Testcontainers. Everything through step 8 was
 * {@code @DataJpaTest} slice tests that never loaded the web layer at all.
 */
class AccountLifecycleIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void exportingARecipeWithRequiredEquipmentDoesNotThrowLazyInitializationException() {
        String email = freshEmail();
        String token = register(email, "password123", freshInviteCode()).getBody().token();
        Long userId = appUserRepository.findByEmail(email).orElseThrow().getId();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(userId, "Milk soup", 20, 2, Set.of("pan", "stovetop"));
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("200"), Unit.GRAM));
        recipeRepository.save(recipe);

        ResponseEntity<AccountExportResponse> response =
                exchangeAuthenticated(token, "/api/account/export", HttpMethod.GET, AccountExportResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().recipes()).hasSize(1);
        assertThat(response.getBody().recipes().getFirst().requiredEquipment()).containsExactlyInAnyOrder("pan", "stovetop");
    }

    @Test
    void registeringWithAValidInviteReturnsATokenAndConsumesTheCode() {
        String code = freshInviteCode();

        ResponseEntity<AuthResponse> response = register(freshEmail(), "password123", code);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(inviteCodeRepository.findByCode(code).orElseThrow().isUsed()).isTrue();
    }

    @Test
    void reusingAnAlreadyConsumedInviteCodeIsRejected() {
        String code = freshInviteCode();
        register(freshEmail(), "password123", code); // consumes it

        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(freshEmail(), "password123", code), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registeringWithAnUnknownInviteCodeIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(freshEmail(), "password123", "no-such-code"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registeringWithAnAlreadyRegisteredEmailIsRejected() {
        String email = freshEmail();
        register(email, "password123", freshInviteCode());

        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "password123", freshInviteCode()), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loggingInWithTheRightPasswordReturnsAToken() {
        String email = freshEmail();
        register(email, "password123", freshInviteCode());

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "password123"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void loggingInWithTheWrongPasswordIsRejected() {
        String email = freshEmail();
        register(email, "password123", freshInviteCode());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(email, "wrong-password"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessingAProtectedEndpointWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/account/export", HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void exportOnlyReturnsTheAuthenticatedUsersOwnData() {
        String emailA = freshEmail();
        String tokenA = register(emailA, "password123", freshInviteCode()).getBody().token();
        String tokenB = register(freshEmail(), "password123", freshInviteCode()).getBody().token();
        givePantryStockFor(emailA, "500");

        AccountExportResponse exportA = exchangeAuthenticated(tokenA, "/api/account/export", HttpMethod.GET, AccountExportResponse.class)
                .getBody();
        AccountExportResponse exportB = exchangeAuthenticated(tokenB, "/api/account/export", HttpMethod.GET, AccountExportResponse.class)
                .getBody();

        assertThat(exportA.pantryItems()).hasSize(1);
        assertThat(exportB.pantryItems()).isEmpty();
    }

    @Test
    void deletingTheAccountRemovesItAndSubsequentAccessNoLongerFindsTheUser() {
        String email = freshEmail();
        String token = register(email, "password123", freshInviteCode()).getBody().token();

        ResponseEntity<Void> deleteResponse = exchangeAuthenticated(token, "/api/account", HttpMethod.DELETE, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(appUserRepository.findByEmail(email)).isEmpty();
        // the JWT's signature is still valid (no revocation list) but the account is gone —
        // the lookup inside AccountService.exportData fails, mapped to 404 by GlobalExceptionHandler.
        ResponseEntity<String> afterDelete = exchangeAuthenticated(token, "/api/account/export", HttpMethod.GET, String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void givePantryStockFor(String email, String quantity) {
        Long userId = appUserRepository.findByEmail(email).orElseThrow().getId();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        pantryService.addStock(userId, milk, new BigDecimal(quantity), Unit.GRAM, LocalDate.now());
    }

    private ResponseEntity<AuthResponse> register(String email, String password, String inviteCode) {
        return restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, password, inviteCode), AuthResponse.class);
    }

    private <T> ResponseEntity<T> exchangeAuthenticated(String token, String path, HttpMethod method, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), responseType);
    }

    private String freshEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String freshInviteCode() {
        String code = "TEST-" + UUID.randomUUID();
        inviteCodeRepository.save(new InviteCode(code));
        return code;
    }
}
