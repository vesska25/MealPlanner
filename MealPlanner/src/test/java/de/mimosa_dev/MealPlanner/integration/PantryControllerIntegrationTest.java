package de.mimosa_dev.MealPlanner.integration;

import de.mimosa_dev.MealPlanner.auth.AppUserRepository;
import de.mimosa_dev.MealPlanner.auth.InviteCode;
import de.mimosa_dev.MealPlanner.auth.InviteCodeRepository;
import de.mimosa_dev.MealPlanner.auth.dto.AuthResponse;
import de.mimosa_dev.MealPlanner.auth.dto.RegisterRequest;
import de.mimosa_dev.MealPlanner.common.Unit;
import de.mimosa_dev.MealPlanner.pantry.DiscardReason;
import de.mimosa_dev.MealPlanner.pantry.PantryItem;
import de.mimosa_dev.MealPlanner.pantry.PantryItemRepository;
import de.mimosa_dev.MealPlanner.pantry.PantryItemStatus;
import de.mimosa_dev.MealPlanner.pantry.PantryService;
import de.mimosa_dev.MealPlanner.pantry.dto.DiscardPantryItemRequest;
import de.mimosa_dev.MealPlanner.pantry.dto.PantryItemResponse;
import de.mimosa_dev.MealPlanner.product.Product;
import de.mimosa_dev.MealPlanner.product.ProductRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PantryControllerIntegrationTest extends AbstractApiIntegrationTest {

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
    private PantryItemRepository pantryItemRepository;

    @Test
    void discardingAnActiveItemMarksItDiscardedWithAReason() {
        Registration registration = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        PantryItem item = pantryService.addStock(registration.userId(), milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());

        ResponseEntity<Void> response = postDiscard(registration.token(), item.getId(), DiscardReason.BOUGHT_TOO_MUCH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        PantryItem reloaded = pantryItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PantryItemStatus.DISCARDED);
        assertThat(reloaded.getDiscardReason()).isEqualTo(DiscardReason.BOUGHT_TOO_MUCH);
    }

    @Test
    void discardingAnAlreadyDiscardedItemIsAConflict() {
        Registration registration = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        PantryItem item = pantryService.addStock(registration.userId(), milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());
        pantryService.discard(registration.userId(), item.getId(), DiscardReason.EXPIRED_EARLY);

        ResponseEntity<Void> response = postDiscard(registration.token(), item.getId(), DiscardReason.BOUGHT_TOO_MUCH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void discardingAnotherUsersItemIsNotFound() {
        Registration owner = register();
        Registration someoneElse = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        PantryItem item = pantryService.addStock(owner.userId(), milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());

        ResponseEntity<Void> response = postDiscard(someoneElse.token(), item.getId(), DiscardReason.BOUGHT_TOO_MUCH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Void> postDiscard(String token, Long itemId, DiscardReason reason) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                "/api/pantry/" + itemId + "/discard", HttpMethod.POST,
                new HttpEntity<>(new DiscardPantryItemRequest(reason), headers), Void.class);
    }

    @Test
    void listsOnlyTheAuthenticatedUsersActiveItems() {
        Registration userA = register();
        Registration userB = register();
        Product milk = productRepository.findByCanonicalNameIgnoreCase("milk").orElseThrow();
        pantryService.addStock(userA.userId(), milk, new BigDecimal("500"), Unit.GRAM, LocalDate.now());

        ResponseEntity<PantryItemResponse[]> responseA = exchangeAuthenticated(userA.token());
        ResponseEntity<PantryItemResponse[]> responseB = exchangeAuthenticated(userB.token());

        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(responseA.getBody())).hasSize(1);
        assertThat(responseA.getBody()[0].productName()).isEqualTo("milk");
        assertThat(List.of(responseB.getBody())).isEmpty();
    }

    @Test
    void accessingWithoutATokenIsRejected() {
        ResponseEntity<String> response = restTemplate.exchange("/api/pantry", HttpMethod.GET, HttpEntity.EMPTY, String.class);

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

    private ResponseEntity<PantryItemResponse[]> exchangeAuthenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/pantry", HttpMethod.GET, new HttpEntity<>(headers), PantryItemResponse[].class);
    }
}
