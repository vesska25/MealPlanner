package de.mimosa_dev.MealPlanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Base for repository tests: real Postgres via Testcontainers, schema from the actual
 * Flyway migrations (ddl-auto is none everywhere, so JPA-generated schema would drift
 * from what production runs).
 *
 * Singleton container pattern deliberately, not {@code @Testcontainers}/{@code @Container}:
 * that annotation pair stops the container in {@code afterAll} for each test class, but
 * Spring's test context cache reuses the same {@code ApplicationContext} (and its already
 * dynamically-resolved datasource properties) across subclasses since they share identical
 * config. A restarted container gets a new mapped port that the cached context never learns
 * about, so the second test class fails to connect. Starting once and never stopping avoids
 * the mismatch; the Testcontainers Ryuk reaper cleans up when the JVM exits.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
public abstract class AbstractIntegrationTest {

    // max_connections bumped from Postgres's default 100: every distinct @Import combination
    // across the test suite gets its own Spring context and HikariCP pool against this same
    // container (Spring's test-context cache holds several at once), and the growing number of
    // test classes eventually exhausted the default (step 10: "sorry, too many clients already").
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withCommand("postgres", "-c", "max_connections=300");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Since step 9 (V8 migration), pantry_item/recipe/agent_run/cooked_dish carry a real FK to
     * app_user — tests that reference a fixed userId literal need a matching row to exist first.
     * A native upsert rather than the {@code AppUser} entity: {@code app_user.id} is
     * identity-generated, and this needs to land on a caller-chosen id, not a generated one.
     *
     * <p>{@code JdbcTemplate} rather than {@code EntityManager}, deliberately: it participates
     * in the ambient Spring-managed transaction when one exists (the normal rolled-back
     * {@code @DataJpaTest} case) but also works standalone in auto-commit mode when a subclass
     * has disabled that transaction (e.g. a concurrency test using
     * {@code @Transactional(propagation = NOT_SUPPORTED)}) — {@code EntityManager.executeUpdate}
     * requires an active transaction either way and fails in the second case.
     */
    protected void ensureUserExists(Long userId) {
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password_hash) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
                userId, "test-user-" + userId + "@example.com", "test-password-hash");
    }
}
