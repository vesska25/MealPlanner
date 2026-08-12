package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
import de.mimosa_dev.MealPlanner.recipe.Recipe;
import de.mimosa_dev.MealPlanner.recipe.RecipeIngredientEntity;
import de.mimosa_dev.MealPlanner.recipe.RecipeRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.shoppinglist.ShoppingListService;
import de.mimosa_dev.MealPlanner.shoppinglist.dto.ShoppingListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListControllerIntegrationTest extends AbstractApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeSuggestionService recipeSuggestionService;

    @Autowired
    private ShoppingListService shoppingListService;

    @Test
    void returnsTheMostRecentlyGeneratedListWithItems() {
        Registration registration = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(registration.userId(), "Milk soup", 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("500"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);
        recipeSuggestionService.activate(registration.userId(), saved.getId(), new BigDecimal("0.5"));
        shoppingListService.generate(registration.userId());

        ResponseEntity<ShoppingListResponse> response = exchangeAuthenticated(registration.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().items()).hasSize(1);
        assertThat(response.getBody().items().getFirst().productName()).isEqualTo("milk");
        assertThat(response.getBody().items().getFirst().block()).isEqualTo("DEFINITELY_NEED");
    }

    @Test
    void returnsNotFoundWhenNoListHasEverBeenGenerated() {
        Registration registration = register();

        ResponseEntity<ShoppingListResponse> response = exchangeAuthenticated(registration.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/shopping-list", HttpMethod.GET, HttpEntity.EMPTY, String.class);

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

    private ResponseEntity<ShoppingListResponse> exchangeAuthenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/api/shopping-list", HttpMethod.GET, new HttpEntity<>(headers), ShoppingListResponse.class);
    }
}
