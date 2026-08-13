# Multi-stage build (PRD acceptance criterion #29: deployed, HTTPS, on a subdomain).
# Tests are NOT run here — they need Testcontainers/Docker-in-Docker, which isn't available
# inside an image build; the real test gate is `mvn test` run separately (CI or manually).

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Cache dependency resolution in its own layer, separate from source changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /build/target/meal-planner-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
