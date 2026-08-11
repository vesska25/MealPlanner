package de.mimosa_dev.MealPlanner.integration;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for the first true end-to-end tests in this project: real HTTP requests through the
 * actual security filter chain, rather than the {@code @DataJpaTest} slice tests used
 * everywhere else in this codebase (those never load the web layer at all). Runs its own
 * Testcontainers Postgres instance, separate from {@code AbstractIntegrationTest}'s — a
 * {@code @DataJpaTest} slice and a full {@code @SpringBootTest} context are different test
 * infrastructures and can't share one static container/context cleanly.
 *
 * <p>Unlike {@code @DataJpaTest}, nothing here rolls back after each test method — an HTTP
 * request runs on a separate server thread with its own transaction, so writes made through the
 * client are real and permanent for the life of the container. Tests create their own
 * self-contained data (e.g. invite codes, distinct emails) rather than relying on shared seed
 * rows or a previous test's state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractApiIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
