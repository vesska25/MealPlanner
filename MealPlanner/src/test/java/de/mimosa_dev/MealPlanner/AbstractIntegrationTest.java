package de.mimosa_dev.MealPlanner;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
