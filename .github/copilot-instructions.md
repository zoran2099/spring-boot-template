# Copilot instructions for `spring-boot-template`

Spring Boot 4.1.0 / Java 21 / Maven starter template. Uses PostgreSQL via JPA and
Testcontainers-driven local development. Base package: `com.example.template`.

## Build, test, run

Always use the Maven wrapper (`./mvnw`) — do **not** rely on a system-wide `mvn`.

- Full build (compile + tests + package): `./mvnw clean verify`
- Compile only: `./mvnw compile`
- Run the app **with Testcontainers-managed Postgres** (preferred for local dev):
  `./mvnw spring-boot:test-run`
  This boots via `TestSpringBootTemplateApplication` and starts the container declared
  in `TestcontainersConfiguration`. There is no `docker-compose` — the container is the
  source of truth for a local Postgres.
- Run the app against an externally-configured DB: `./mvnw spring-boot:run`
  (you must supply `spring.datasource.*` yourself; the main `application.properties`
  intentionally does not configure a datasource).
- Full test suite: `./mvnw test`
- Single test class: `./mvnw test -Dtest=SpringBootTemplateApplicationTests`
- Single test method: `./mvnw test -Dtest=SpringBootTemplateApplicationTests#contextLoads`
- Build an OCI image: `./mvnw spring-boot:build-image`

Docker must be running for any `spring-boot:test-run` or `@SpringBootTest` that pulls in
`TestcontainersConfiguration`, otherwise the context will fail to start. The Testcontainers
**Ryuk resource-reaper is disabled** via `<environmentVariables>` in the surefire plugin
(see `pom.xml`) because it is incompatible with Docker daemons that do not expose their
ports directly to the host (Rancher Desktop, remote/SSH Docker, WSL2 without
`host.docker.internal`, etc.). Do not "clean this up" — the JVM shutdown hook still
stops the containers.

If you run tests **outside Maven** (IDE, standalone), export
`TESTCONTAINERS_RYUK_DISABLED=true` or add a JVM flag `-Dtestcontainers.ryuk.disabled=true`.
The `src/test/resources/testcontainers.properties` file documents this fallback but is
not effective under Surefire — the env var/system property is what actually takes effect.

## Architecture: Testcontainers as a dev-time service

This template intentionally uses Spring Boot 4's "dev services" pattern instead of an
`application-dev.properties` datasource. Understanding this requires reading three files
together:

- `src/main/java/com/example/template/SpringBootTemplateApplication.java` — the real
  production entry point. Has no DB config.
- `src/test/java/com/example/template/TestcontainersConfiguration.java` — a
  `@TestConfiguration` that declares a `PostgreSQLContainer` `@Bean` annotated with
  `@ServiceConnection`. `@ServiceConnection` is what wires the container's JDBC URL /
  credentials into Spring's `DataSource` automatically; **do not** manually set
  `spring.datasource.url` in tests.
- `src/test/java/com/example/template/TestSpringBootTemplateApplication.java` — a
  `main()` under `src/test` that runs the production app but layers
  `TestcontainersConfiguration` on top via `SpringApplication.from(...).with(...)`.
  This is what `spring-boot:test-run` invokes.

When adding new external services (Redis, Kafka, etc.), follow the same pattern:
declare a `@Bean` container in `TestcontainersConfiguration` with `@ServiceConnection`,
and it will be picked up by both `@SpringBootTest` and `spring-boot:test-run`.

## Spring Boot 4.x starter naming (important)

This project is on Spring Boot **4.1.0** (note: the `pom.xml` uses `4.1.0`, **not**
`4.1.0.RELEASE` — the Spring Initializr generates the `.RELEASE` suffix but that exact
artifact is not published to Maven Central; always strip the `.RELEASE` suffix from
future Boot 4.x bumps). Boot 4.x renamed/split several starters. Do not "correct"
these to their 3.x names:

- Web MVC starter is `spring-boot-starter-webmvc` (not `spring-boot-starter-web`).
- Test starters are **split per concern** rather than a single `spring-boot-starter-test`:
  `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`,
  `spring-boot-starter-validation-test`, `spring-boot-starter-actuator-test`.
  When adding tests for a new concern, add the matching `*-test` starter.

## Lombok

Lombok is enabled and wired as an annotation processor in **both** the `default-compile`
and `default-testCompile` executions of `maven-compiler-plugin` (see `pom.xml`). When
adding new annotation processors (MapStruct, etc.), they must be added to **both**
executions or test compilation will break.

## Conventions

- Package everything under `com.example.template.*`. The `@SpringBootApplication` sits at
  the root of that package and relies on component scan — new `@Component`,
  `@Service`, `@Repository`, `@Controller`, `@Configuration` classes must live under it.
- `application.properties` is deliberately minimal (only `spring.application.name`).
  Environment-specific config belongs in profile-specific files
  (`application-<profile>.properties`) or externalized config, not in the base file.
- Actuator is on the classpath but no endpoints are explicitly exposed; if you enable
  more, update `management.endpoints.web.exposure.include` explicitly.
- `postgresql` is `runtime`-scoped in `pom.xml` — do not import PG-specific classes
  in `src/main`; keep the code DB-agnostic at the JPA level.

## Reference

Auto-generated `HELP.md` at the repo root lists the Spring Boot 4.1.0 reference doc
links for each included starter — consult it before searching the web for API details.

## Recommended Copilot CLI plugins

This template expects the following plugins from the built-in `awesome-copilot`
marketplace (`github/awesome-copilot`). They ship the skills that Copilot loads
automatically when it detects Java / Spring / testing / OpenAPI / security context.
Install them once per machine:

```bash
copilot plugin install java-development@awesome-copilot
copilot plugin install testing-automation@awesome-copilot
copilot plugin install security-best-practices@awesome-copilot
copilot plugin install modernize-java@awesome-copilot
copilot plugin install openapi-to-application-java-spring-boot@awesome-copilot
```

What each provides in the context of this repo:

- **`java-development`** — Spring Boot 4 + JUnit 6 + AssertJ testing skill, general
  Spring Boot best practices, Javadoc conventions, Java 21 idioms. This is the
  primary skill set for day-to-day work here.
- **`testing-automation`** — JUnit 5/6, Testcontainers, and integration/E2E testing
  patterns; aligns with the `TestcontainersConfiguration` dev-services pattern above.
- **`security-best-practices`** — baseline security review guidance for JPA / web
  code (input validation, SQL injection via JPQL, secret handling).
- **`modernize-java`** — CLI-driven upgrade plans for future Java (21 → 25) and
  Spring Boot version bumps; use when doing dependency modernization.
- **`openapi-to-application-java-spring-boot`** — scaffold controllers/services from
  an OpenAPI spec into this project's `com.example.template` package layout.

Verify with `copilot plugin list`. If the marketplace is not registered
(older CLI), run `copilot plugin marketplace add github/awesome-copilot` first.
