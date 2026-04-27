# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Educational monorepo implementing Clean/Hexagonal Architecture with Java 21, Spring Boot 4.0.5, PostgreSQL, Docker, and a two-stage CI/CD pipeline on Azure DevOps (GitHub as source).

---

## Common Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=GetProductServiceTest

# Run a single test method
./mvnw test -Dtest=GetProductServiceTest#shouldReturnProductWhenExistsById

# Generate JaCoCo coverage report (output: target/site/jacoco/index.html)
./mvnw test jacoco:report

# Check code formatting (Spotless)
./mvnw spotless:check

# Apply code formatting
./mvnw spotless:apply

# Start local stack (PostgreSQL + app)
docker compose -f deploy/compose.yaml up -d --build

# Stop local stack
docker compose -f deploy/compose.yaml down
```

---

## Architecture

The codebase follows **Ports & Adapters (Hexagonal Architecture)** with three layers under `com.acme.cicd.product`:

```
domain/          — Pure Java records (Product). No Spring, no JPA.
application/     — Use cases (services + port interfaces).
                   port/in/  → inbound interfaces (GetProductUseCase, CreateProductUseCase)
                   port/out/ → outbound interfaces (ProductRepositoryPort)
infrastructure/  — Spring + JPA adapters.
                   web/        → REST controllers + DTOs + RestExceptionHandler
                   persistence/ → JPA entity, Spring Data repo, ProductPersistenceAdapter
                   config/     → Spring @Configuration for bean wiring
```

**Dependency rule (enforced by ArchUnit):**
- `domain` has zero external dependencies — no Spring, no JPA.
- `application` depends only on `domain` and its own port interfaces — never on `infrastructure`.
- `infrastructure` depends on `application` ports to wire adapters.

Violations of these rules will be caught by `ArchitectureTest` (8 architecture tests).

---

## API Endpoints

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| POST | `/products` | Create product | 201 / 409 |
| GET | `/products/{id}` | Get by UUID | 200 / 404 |
| GET | `/products?sku=SKU-001` | Get by SKU | 200 / 404 |
| GET | `/actuator/health` | Health check | 200 |
| GET | `/swagger-ui.html` | API docs | 200 |

---

## Database

- **PostgreSQL 16** managed by **Flyway** (schema versioned under `src/main/resources/db/migration/`).
- `hibernate.ddl-auto=none` — schema is never auto-generated; always use Flyway migrations.
- Default local credentials (Docker): `user=app`, `password=app`, `db=cicd_db`, `port=5432`.
- Configurable via env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`.

---

## Testing

| Test class | Type | Count |
|------------|------|-------|
| `GetProductServiceTest` | Unit (Mockito) | 4 |
| `CreateProductServiceTest` | Unit (Mockito) | 3 |
| `ArchitectureTest` | Architecture (ArchUnit) | 8 |
| `ProductsControllerIntegrationTest` | Integration (TestContainers) | — |

Unit tests use `@ExtendWith(MockitoExtension.class)` with mocked `ProductRepositoryPort`. Architecture tests assert layer dependency rules. Integration tests spin up a real PostgreSQL container via TestContainers.

---

## CI/CD Pipeline (`azure-pipelines.yml`)

**Stage 1 — CI** (`ubuntu-latest`, Microsoft-hosted):
1. Cache Maven dependencies.
2. `mvn clean package -DskipTests`.

**Stage 2 — CD** (`selfhost-linux`, self-hosted agent — only on `main`):
1. Build JAR.
2. `docker compose build`.
3. `docker compose up -d --remove-orphans`.
4. Run smoke tests (`deploy/smoke.sh`).

The Docker image uses `eclipse-temurin:21-jre`, copies the JAR from `target/`, and exposes port `8080`.
