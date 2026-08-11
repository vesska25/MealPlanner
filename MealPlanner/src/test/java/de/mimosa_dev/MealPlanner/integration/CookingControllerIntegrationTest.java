package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.cooking.dto.CookedDishResponse;
import de.mimosa_dev.MealPlanner.cooking.dto.ConfirmCookingRequest;
import de.mimosa_dev.MealPlanner.cooking.dto.ConsumePortionsRequest;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CookingControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PantryService pantryService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void confirmingCookingCreatesAndListsACookedDish() {
        Registration registration = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        pantryService.addStock(registration.userId(), milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());
        Recipe recipe = new Recipe(registration.userId(), "Milk soup", 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("200"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);

        ResponseEntity<CookedDishResponse> confirmResponse = exchangeAuthenticated(
                registration.token(), "/api/cooking/confirm", HttpMethod.POST,
                new ConfirmCookingRequest(saved.getId(), new BigDecimal("2"), "key-" + UUID.randomUUID()),
                CookedDishResponse.class);

        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmResponse.getBody().recipeName()).isEqualTo("Milk soup");

        ResponseEntity<CookedDishResponse[]> listResponse = exchangeAuthenticated(
                registration.token(), "/api/cooked-dishes", HttpMethod.GET, null, CookedDishResponse[].class);
        assertThat(List.of(listResponse.getBody())).hasSize(1);
    }

    @Test
    void consumingPortionsReducesTheRemainder() {
        Registration registration = register();
        Long dishId = cookADish(registration);

        ResponseEntity<Void> response = exchangeAuthenticated(
                registration.token(), "/api/cooked-dishes/" + dishId + "/consume", HttpMethod.POST,
                new ConsumePortionsRequest(new BigDecimal("1")), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void discardingACookedDishSucceeds() {
        Registration registration = register();
        Long dishId = cookADish(registration);

        ResponseEntity<Void> response = exchangeAuthenticated(
                registration.token(), "/api/cooked-dishes/" + dishId, HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void anotherUsersCookedDishCannotBeConsumed() {
        Registration owner = register();
        Registration someoneElse = register();
        Long dishId = cookADish(owner);

        ResponseEntity<Void> response = exchangeAuthenticated(
                someoneElse.token(), "/api/cooked-dishes/" + dishId + "/consume", HttpMethod.POST,
                new ConsumePortionsRequest(new BigDecimal("1")), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/cooked-dishes", HttpMethod.GET, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Long cookADish(Registration registration) {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        pantryService.addStock(registration.userId(), milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());
        Recipe recipe = new Recipe(registration.userId(), "Milk soup " + UUID.randomUUID(), 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("200"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);

        ResponseEntity<CookedDishResponse> response = exchangeAuthenticated(
                registration.token(), "/api/cooking/confirm", HttpMethod.POST,
                new ConfirmCookingRequest(saved.getId(), new BigDecimal("2"), "key-" + UUID.randomUUID()),
                CookedDishResponse.class);
        return response.getBody().id();
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
