# Copilot instructions for `catalog-api`

Java 21 / Spring Boot 4.1 / Maven application. Base package:
`br.com.bbts.catalog`.

## Architecture invariants

- The public catalog API is read-only and implemented with Spring Data REST.
- Public repositories must be explicitly annotated with `@RepositoryRestResource`.
- Keep `POST`, `PUT`, `PATCH`, and `DELETE` disabled globally in
  `RestRepositoryConfiguration`.
- Responses use HAL under `/api/v1`; keep pagination capped at 100 items.
- Use projections for entity-shaped enriched responses. Introduce DTOs and
  controllers only for use cases that aggregate multiple domains or external data.
- Basic Auth credentials must come from external configuration. Never add defaults,
  secrets, or passwords to tracked property files.
- Catalog GET endpoints require `READER` or `ADMIN`. Swagger and administrative
  Actuator endpoints require `ADMIN`; health probes remain public.
- The `local` profile may use Hibernate schema update. The `prod` profile must
  validate a schema provisioned outside the application.
- Compose is the full local runtime; Testcontainers remains the integration-test
  infrastructure. PostgreSQL must not publish a host port from Compose.

## Build and test

Always use `./mvnw`:

- `./mvnw clean verify`
- `./mvnw spring-boot:test-run` for Testcontainers-managed local execution
- `docker compose --env-file .env.example config`
- `docker compose up --build` for the full local stack

Docker must be available for integration tests. Testcontainers Ryuk remains disabled
by the Maven Surefire configuration for compatibility with corporate/remote daemons.

## Dependencies and conventions

- Spring Boot 4 uses `spring-boot-starter-webmvc` and split concern-specific test
  starters; do not replace them with Boot 3 artifact names.
- Lombok is configured as an annotation processor for main and test compilation.
  Add future annotation processors to both Maven compiler executions.
- Keep application code under `br.com.bbts.catalog.*` so component and entity scans
  originate from `CatalogApiApplication`.
- Use PostgreSQL-compatible mappings and Testcontainers for repository behavior.
- Preserve ADRs 0001–0009 as template history. Record changed decisions in successor
  ADRs instead of rewriting accepted historical records.
