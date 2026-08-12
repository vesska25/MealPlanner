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
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestion;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionRepository;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionService;
import de.mimosa_dev.MealPlanner.recipe.RecipeSuggestionStatus;
import de.mimosa_dev.MealPlanner.recipe.RejectionReason;
import de.mimosa_dev.MealPlanner.recipe.dto.ActiveSuggestionResponse;
import de.mimosa_dev.MealPlanner.recipe.dto.RejectSuggestionRequest;
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

class RecipeControllerIntegrationTest extends AbstractApiIntegrationTest {

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
    private RecipeSuggestionRepository recipeSuggestionRepository;

    @Test
    void acceptingAnActiveSuggestionMarksItAccepted() {
        Registration registration = register();
        Long recipeId = recipeWithSuggestion(registration);

        ResponseEntity<Void> response = postAuthenticated(
                registration.token(), "/api/recipes/suggestions/" + recipeId + "/accept", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        RecipeSuggestion suggestion = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(registration.userId(), recipeId, RecipeSuggestionStatus.ACCEPTED)
                .orElseThrow();
        assertThat(suggestion.getStatus()).isEqualTo(RecipeSuggestionStatus.ACCEPTED);
    }

    @Test
    void acceptingWhenThereIsNoActiveSuggestionForThatRecipeIsNotFound() {
        Registration registration = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(registration.userId(), "Milk soup", 20, 2, Set.of());
        Recipe saved = recipeRepository.save(recipe);

        ResponseEntity<Void> response = postAuthenticated(
                registration.token(), "/api/recipes/suggestions/" + saved.getId() + "/accept", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectingAnActiveSuggestionRecordsTheReason() {
        Registration registration = register();
        Long recipeId = recipeWithSuggestion(registration);

        ResponseEntity<Void> response = postAuthenticated(
                registration.token(), "/api/recipes/suggestions/" + recipeId + "/reject",
                new RejectSuggestionRequest(RejectionReason.DISLIKE_DISH), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        RecipeSuggestion suggestion = recipeSuggestionRepository
                .findByUserIdAndRecipeIdAndStatus(registration.userId(), recipeId, RecipeSuggestionStatus.REJECTED)
                .orElseThrow();
        assertThat(suggestion.getRejectionReason()).isEqualTo(RejectionReason.DISLIKE_DISH);
    }

    @Test
    void rejectingWhenThereIsNoActiveSuggestionIsNotFound() {
        Registration registration = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(registration.userId(), "Milk soup", 20, 2, Set.of());
        Recipe saved = recipeRepository.save(recipe);

        ResponseEntity<Void> response = postAuthenticated(
                registration.token(), "/api/recipes/suggestions/" + saved.getId() + "/reject",
                new RejectSuggestionRequest(RejectionReason.DISLIKE_DISH), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long recipeWithSuggestion(Registration registration) {
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(registration.userId(), "Milk soup " + UUID.randomUUID(), 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("200"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);
        recipeSuggestionService.activate(registration.userId(), saved.getId(), new BigDecimal("0.6"));
        return saved.getId();
    }

    @Test
    void returnsTheActiveSuggestionWithIngredients() {
        Registration registration = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        Recipe recipe = new Recipe(registration.userId(), "Milk soup", 20, 2, Set.of());
        recipe.addIngredient(new RecipeIngredientEntity(milk, new BigDecimal("200"), Unit.GRAM));
        Recipe saved = recipeRepository.save(recipe);
        recipeSuggestionService.activate(registration.userId(), saved.getId(), new BigDecimal("0.6"));

        ResponseEntity<ActiveSuggestionResponse> response = exchangeAuthenticated(registration.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().recipeName()).isEqualTo("Milk soup");
        assertThat(response.getBody().ingredients()).hasSize(1);
    }

    @Test
    void returnsNotFoundWhenThereIsNoActiveSuggestion() {
        Registration registration = register();

        ResponseEntity<ActiveSuggestionResponse> response = exchangeAuthenticated(registration.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/recipes/suggestions/active", HttpMethod.GET, HttpEntity.EMPTY, String.class);

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

    private ResponseEntity<ActiveSuggestionResponse> exchangeAuthenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/api/recipes/suggestions/active", HttpMethod.GET, new HttpEntity<>(headers), ActiveSuggestionResponse.class);
    }

    private <T> ResponseEntity<T> postAuthenticated(String token, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }
}
