# Spring Boot CI/CD — Azure DevOps

Proyecto educativo que implementa **Hexagonal Architecture** con Java 21 + Spring Boot 4.0.5 + PostgreSQL, con un pipeline CI/CD completo en Azure DevOps que bloquea merges a `main` si el pipeline falla.

---

## Stack

| Tecnología | Versión | Rol |
|-----------|---------|-----|
| Java | 21 | Lenguaje |
| Spring Boot | 4.0.5 | Framework web + DI |
| PostgreSQL | 16 | Base de datos |
| Flyway | incluido en Boot | Migraciones de BD |
| TestContainers | 1.21.0 | Tests de integración con BD real |
| ArchUnit | 1.4.1 | Validación de arquitectura |
| JaCoCo | 0.8.13 | Cobertura de código (mín. 70%) |
| Spotless | 2.44.0 | Formato de código (Google Java Format) |
| Docker + Compose | 27.5.1 | Contenedores |
| Azure DevOps | - | CI/CD pipeline |

---

## Prerequisitos

- **Java 21+** instalado
- **Docker Engine** corriendo (Linux/WSL2) o Docker Desktop (Mac/Windows)
- **Git**

No necesitas instalar Maven — el proyecto incluye `mvnw` (Maven Wrapper).

---

## Ejecución local rápida

### 1. Clonar

```bash
git clone https://github.com/LoraDev010/spring-boot-cicd-azure-devops.git
cd spring-boot-cicd-azure-devops
```

### 2. Levantar con Docker Compose (recomendado)

```bash
# Construir imagen + levantar PostgreSQL + app
docker compose -f deploy/compose.yaml up -d --build

# Verificar que todo está OK
bash deploy/smoke.sh
# Debe mostrar: Smoke OK
```

### 3. Levantar solo la app (requiere PostgreSQL externo)

```bash
# Compilar
./mvnw clean package -DskipTests

# Levantar (con variables de entorno apuntando a tu PostgreSQL)
DB_HOST=localhost DB_PORT=5432 DB_NAME=cicd_db DB_USER=app DB_PASS=app \
  java -jar target/*.jar
```

---

## API

Base URL: `http://localhost:8080`

### Crear producto

```bash
POST /products
Content-Type: application/json

{
  "sku": "SKU-001",
  "name": "Laptop Pro",
  "price": 1299.99,
  "currency": "USD",
  "stock": 50
}

# Response 201 Created
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sku": "SKU-001",
  "name": "Laptop Pro",
  "price": 1299.99,
  "currency": "USD",
  "stock": 50
}
```

### Obtener por ID

```bash
GET /products/{id}
# Response 200 OK — mismo formato que arriba
# Response 404 si no existe
```

### Obtener por SKU

```bash
GET /products?sku=SKU-001
# Response 200 OK — mismo formato que arriba
# Response 404 si no existe
```

### Health check

```bash
GET /actuator/health
# Response: {"status":"UP"}
```

### Documentación interactiva (Swagger)

```
http://localhost:8080/swagger-ui.html
```

### Errores

```json
// 409 Conflict — SKU duplicado
{ "status": 409, "title": "Conflict", "detail": "sku already exists" }

// 404 Not Found
{ "status": 404, "title": "Not Found", "detail": "product not found" }

// 400 Bad Request — validación
{ "status": 400, "title": "Bad Request", "detail": "..." }
```

---

## Tests

```bash
# Todos los tests (unitarios + arquitectura + integración)
# Requiere Docker corriendo (TestContainers levanta PostgreSQL real)
./mvnw test

# Ciclo completo igual al pipeline CI (tests + formato + cobertura)
./mvnw verify

# Test específico
./mvnw test -Dtest=GetProductServiceTest
./mvnw test -Dtest=CreateProductServiceTest#shouldCreateProductWhenSkuDoesNotExist

# Reporte de cobertura (genera target/site/jacoco/index.html)
./mvnw test jacoco:report

# Corregir formato de código
./mvnw spotless:apply
```

### Clases de test

| Clase | Tipo | Descripción |
|-------|------|-------------|
| `GetProductServiceTest` | Unit (Mockito) | getById, getBySku, excepciones |
| `CreateProductServiceTest` | Unit (Mockito) | crear producto, SKU duplicado, UUID único |
| `ArchitectureTest` | Arquitectura (ArchUnit) | 8 reglas de dependencias entre capas |
| `ProductsControllerIntegrationTest` | Integración (TestContainers) | flujo completo HTTP → BD → respuesta |

---

## Arquitectura

```
com.acme.cicd.product/
├── domain/          ← Java puro. Sin Spring, sin JPA. Solo records.
├── application/     ← Casos de uso + interfaces de puertos (in/out)
└── infrastructure/  ← Adaptadores: REST controller, JPA, config de beans
```

**Regla de dependencias (verificada por ArchUnit en cada build):**

```
infrastructure → application → domain
```

`domain` no conoce Spring. `application` no conoce JPA. Violaciones = test falla = CI falla = merge bloqueado.

---

## Migraciones de BD (Flyway)

```
src/main/resources/db/migration/
├── V1__create_products.sql          ← Crea tabla products
└── V2__add_stock_to_products.sql    ← Agrega columna stock
```

`hibernate.ddl-auto=none` — el schema NUNCA se genera automáticamente. Solo Flyway.

---

## Pipeline CI/CD

```
Push/PR → Stage CI → Stage CD (solo en main)
```

**Stage CI** (corre en PR y en push a main):
- `./mvnw verify` → compila + tests + formato Spotless + cobertura JaCoCo (mín 70%)
- Si falla → merge bloqueado en GitHub

**Stage CD** (solo en merge a main):
- Compila JAR
- `docker compose build` → nueva imagen
- `docker compose up -d` → reemplaza contenedores
- `bash deploy/smoke.sh` → valida que la app responde

**Branch Protection (`main`):**
- Push directo bloqueado
- PR obligatorio con 1 aprobación
- Pipeline CI debe pasar antes de poder mergear

---

## Comandos útiles

```bash
# Compilar sin tests
./mvnw clean package -DskipTests

# Docker Compose
docker compose -f deploy/compose.yaml up -d --build   # Levantar
docker compose -f deploy/compose.yaml down             # Detener
docker compose -f deploy/compose.yaml down -v          # Detener + borrar BD
docker compose -f deploy/compose.yaml logs -f app      # Ver logs

# Smoke test manual
bash deploy/smoke.sh
```

---

## Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_PORT` | `5432` | Puerto PostgreSQL |
| `DB_NAME` | `cicd_db` | Nombre de la BD |
| `DB_USER` | `app` | Usuario |
| `DB_PASS` | `app` | Contraseña |

---

## Estructura de archivos

```
.
├── azure-pipelines.yml              # Pipeline CI/CD
├── Dockerfile                       # Imagen: eclipse-temurin:21-jre
├── pom.xml                          # Dependencias y plugins Maven
├── mvnw                             # Maven Wrapper (no necesitas instalar Maven)
├── deploy/
│   ├── compose.yaml                 # PostgreSQL 16 + Spring Boot app
│   └── smoke.sh                     # Smoke tests post-deploy
└── src/
    ├── main/
    │   ├── java/com/acme/cicd/      # Código fuente
    │   └── resources/
    │       ├── application.yaml
    │       └── db/migration/        # Migraciones Flyway
    └── test/
        └── java/com/acme/cicd/      # Tests
```
