# Mini CI/CD Azure DevOps - Guía de Configuración

## 🎯 Descripción del Proyecto

Proyecto educativo implementando **Clean Architecture + SOLID + Tests automatizados** en Java 21 con Spring Boot 4.0.5.

**Características principales:**
- ✅ Clean/Hexagonal Architecture con 3 capas (domain, application, infrastructure)
- ✅ API REST con CRUD de productos
- ✅ PostgreSQL + Flyway (migraciones versionadas)
- ✅ Tests: Unit (Mockito) + Architecture (ArchUnit)
- ✅ Docker Compose (local development)
- ✅ Azure DevOps YAML pipeline (CI en ubuntu-latest, CD en self-hosted Linux)
- ✅ Swagger UI (OpenAPI) en `/swagger-ui.html`

---

## 📋 Requisitos Previos

### Opción 1: Solo código (sin Docker)
- **Java 21+** - [Descargar](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.9+** - Incluido (mvnw.cmd/mvnw)
- **Git** - Para clonar el repo

### Opción 2: Con Docker (recomendado)
- **Docker Desktop** - [Descargar](https://www.docker.com/products/docker-desktop)
- **Docker Compose** - Incluido en Docker Desktop
- **Git**

---

## 🚀 Instalación y Ejecución

### 1️⃣ Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/mini-cicd-azuredevops.git
cd mini-cicd-azuredevops
```

---

### 2️⃣ Opción A: Compilar y Ejecutar Tests (sin BD)

#### Windows (CMD)
```bash
# Verificar Maven
.\mvnw.cmd -v

# Compilar
.\mvnw.cmd clean package -DskipTests

# Tests unitarios + architecture
.\mvnw.cmd test

# O solo tests específicos
.\mvnw.cmd test -Dtest=GetProductServiceTest
.\mvnw.cmd test -Dtest=CreateProductServiceTest
.\mvnw.cmd test -Dtest=ArchitectureTest
```

#### Linux/Mac
```bash
# Verificar Maven
./mvnw -v

# Compilar
./mvnw clean package -DskipTests

# Tests
./mvnw test
```

---

### 3️⃣ Opción B: Docker Compose (Local Development - Recomendado)

#### Windows (PowerShell)
```powershell
# Posicionarse en el directorio del proyecto
cd mini-cicd-azuredevops

# Construir imagen Docker + levantar contenedores
docker compose -f deploy/compose.yaml up -d --build

# Esperar ~10 segundos a que PostgreSQL inicie

# Ejecutar smoke tests (valida que todo esté OK)
bash deploy/smoke.sh

# Ver logs
docker compose -f deploy/compose.yaml logs app

# Detener servicios
docker compose -f deploy/compose.yaml down
```

#### Linux/Mac
```bash
cd mini-cicd-azuredevops
docker compose -f deploy/compose.yaml up -d --build
sleep 10
bash deploy/smoke.sh
docker compose -f deploy/compose.yaml logs app
docker compose -f deploy/compose.yaml down
```

---

## 📊 Estructura del Proyecto

```
mini-cicd-azuredevops/
├── pom.xml                                  # Configuración Maven
├── mvnw / mvnw.cmd                         # Maven Wrapper (sin install global)
├── .mvn/                                   # Config del wrapper
│
├── src/
│   ├── main/
│   │   ├── java/com/acme/cicd/
│   │   │   ├── Application.java            # Entry point Spring Boot
│   │   │   └── product/
│   │   │       ├── domain/                 # CORE: Sin Spring, sin JPA
│   │   │       │   └── Product.java        # Entidad de negocio
│   │   │       │
│   │   │       ├── application/            # Casos de uso
│   │   │       │   ├── CreateProductService.java
│   │   │       │   ├── GetProductService.java
│   │   │       │   ├── DuplicateSkuException.java
│   │   │       │   ├── ProductNotFoundException.java
│   │   │       │   └── port/               # Puertos (interfaces)
│   │   │       │       ├── in/             # Puertos de entrada
│   │   │       │       │   ├── CreateProductUseCase.java
│   │   │       │       │   ├── CreateProductCommand.java
│   │   │       │       │   └── GetProductUseCase.java
│   │   │       │       └── out/            # Puertos de salida
│   │   │       │           └── ProductRepositoryPort.java
│   │   │       │
│   │   │       └── infrastructure/         # Adaptadores (Spring, JPA, etc)
│   │   │           ├── web/                # HTTP Adapters
│   │   │           │   ├── ProductsController.java
│   │   │           │   ├── CreateProductRequest.java
│   │   │           │   ├── ProductResponse.java
│   │   │           │   └── RestExceptionHandler.java
│   │   │           │
│   │   │           └── persistence/        # Persistencia Adapters
│   │   │               └── jpa/
│   │   │                   ├── ProductJpaEntity.java
│   │   │                   ├── ProductJpaRepository.java
│   │   │                   └── ProductPersistenceAdapter.java
│   │   │
│   │   └── resources/
│   │       ├── application.yaml            # Config (DB, puertos, etc)
│   │       └── db/migration/
│   │           └── V1__create_products.sql # Flyway migration
│   │
│   └── test/
│       └── java/com/acme/cicd/product/
│           ├── application/
│           │   ├── GetProductServiceTest.java      # Unit tests Mockito
│           │   └── CreateProductServiceTest.java
│           │
│           └── ArchitectureTest.java               # Tests de arquitectura
│
├── deploy/
│   ├── compose.yaml                 # Docker Compose (PostgreSQL + App)
│   ├── smoke.sh                     # Smoke tests (validación E2E)
│   └── .env.example                 # Variables de entorno
│
├── Dockerfile                       # Imagen Spring Boot
├── azure-pipelines.yml              # CI/CD Pipeline (Azure DevOps)
├── .github/                         # GitHub workflows (opcional)
└── README.md                        # Este archivo
```

---

## 🔧 Endpoints Disponibles

### Health Check
```bash
GET http://localhost:8080/actuator/health
```

### API Productos

#### Crear producto
```bash
POST /products
Content-Type: application/json

{
  "sku": "SKU-001",
  "name": "Laptop Pro",
  "price": 1299.99,
  "currency": "USD"
}

# Response (201 Created)
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sku": "SKU-001",
  "name": "Laptop Pro",
  "price": 1299.99,
  "currency": "USD"
}
```

#### Obtener por ID
```bash
GET /products/{id}

# Response (200 OK)
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sku": "SKU-001",
  "name": "Laptop Pro",
  "price": 1299.99,
  "currency": "USD"
}
```

#### Obtener por SKU
```bash
GET /products?sku=SKU-001

# Response (200 OK)
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sku": "SKU-001",
  "name": "Laptop Pro",
  "price": 1299.99,
  "currency": "USD"
}
```

### Errores (Problem Details)
```bash
# Si SKU ya existe (409 Conflict)
{
  "status": 409,
  "title": "Conflict",
  "detail": "sku already exists"
}

# Si producto no encontrado (404 Not Found)
{
  "status": 404,
  "title": "Not Found",
  "detail": "product not found"
}
```

---

## ✅ Tests

### Ejecutar todos los tests
```bash
# Windows
.\mvnw.cmd test

# Linux/Mac
./mvnw test
```

### Tests disponibles

| Clase | Tests | Descripción |
|-------|-------|-------------|
| `GetProductServiceTest` | 4 | Unit tests: getById, getBySku, excepciones |
| `CreateProductServiceTest` | 3 | Unit tests: crear producto, validar SKU único |
| `ArchitectureTest` | 8 | Validar reglas Clean Architecture |
| **TOTAL** | **15** | ✅ Todos pasando |

### Ejecutar test específico
```bash
# Windows
.\mvnw.cmd test -Dtest=GetProductServiceTest

# Linux/Mac
./mvnw test -Dtest=GetProductServiceTest
```

### Cobertura (JaCoCo)
```bash
.\mvnw.cmd test jacoco:report

# Ver reporte en: target/site/jacoco/index.html
```

---

## 🐳 Docker Compose

### Servicios levantados

| Servicio | Puerto | Usuario | Password |
|----------|--------|---------|----------|
| PostgreSQL | 5432 | app | app |
| Spring Boot App | 8080 | - | - |

### Variables de entorno (`.env`)
```env
DB_HOST=postgres
DB_PORT=5432
DB_NAME=cicd_db
DB_USER=app
DB_PASS=app
```

### Comandos útiles
```bash
# Levantar en background
docker compose -f deploy/compose.yaml up -d --build

# Ver logs
docker compose -f deploy/compose.yaml logs -f app

# Detener
docker compose -f deploy/compose.yaml down

# Limpiar volúmenes (borra BD)
docker compose -f deploy/compose.yaml down -v

# Ejecutar comando en contenedor
docker compose -f deploy/compose.yaml exec app ./mvnw test
```

---

## 📋 CI/CD (Azure DevOps)

### Pipeline stages

#### Stage 1: CI (ubuntu-latest)
- Cache Maven
- `./mvnw spotless:check` (validar formato)
- `./mvnw test` (tests unitarios + architecture)
- `./mvnw verify -PIT` (integration tests)
- Publicar reportes JUnit + JaCoCo

#### Stage 2: CD (self-hosted Linux)
- Checkout código
- `docker compose build`
- `docker compose up -d`
- Ejecutar smoke tests
- Si falla: logs + rollback

### Activar en Azure DevOps
1. Crear proyecto en [dev.azure.com](https://dev.azure.com)
2. Importar repo (GitHub)
3. Crear pipeline → seleccionar `azure-pipelines.yml`
4. Configurar self-hosted agent (si quieres CD)

---

## 🏗️ Conceptos Implementados

### Clean Architecture (3 capas)
```
Domain (sin Spring/JPA)
    ↓ (depende)
Application (casos de uso, puertos)
    ↓ (depende)
Infrastructure (controllers, JPA, HTTP)
```

**Regla:** Domain ≠ Spring, Application ≠ Infrastructure

### SOLID
- **S**ingle Responsibility: Cada servicio = 1 responsabilidad
- **O**pen/Closed: Interfaces de puertos (productibles)
- **L**iskov: Implementaciones intercambiables
- **I**nterface Segregation: Puertos pequeños y específicos
- **D**ependency Inversion: Inyección de puertos (interfaces)

### Ports & Adapters (Hexagonal)
```
CreateProductUseCase (In-Port)
    ↓
CreateProductService (Implementación)
    ↓
ProductRepositoryPort (Out-Port)
    ↓
ProductPersistenceAdapter (Implementación JPA)
```

---

## 📝 Tecnologías

| Stack | Versión |
|-------|---------|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Maven | 3.9+ (via mvnw) |
| PostgreSQL | 16 (Docker) |
| Flyway | 9.x |
| JUnit 5 | 5.9+ |
| Mockito | 5.x |
| ArchUnit | 1.4.1 |
| Docker | 20.10+ |
| Azure DevOps | Gratis |

---

## 🔍 Troubleshooting

### Error: "Puerto 5432 en uso"
```bash
# Matar proceso usando puerto 5432
# Windows
netstat -ano | findstr :5432
taskkill /PID <PID> /F

# Linux
lsof -i :5432
kill -9 <PID>

# Docker
docker compose down -v
```

### Error: "Database does not exist"
```bash
# Flyway no corrió. Verificar logs
docker compose logs postgres
docker compose logs app

# Reintentar
docker compose down -v
docker compose up -d --build
```

### Tests fallan en CI/CD pero pasan local
```bash
# Asegurar Maven cache limpio
.\mvnw.cmd clean test

# Verificar JDK 21
java -version

# Ejecutar en Azure: misma versión de Java
```

---

## 📚 Referencias

- [Clean Architecture - Uncle Bob](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Spring Boot Official](https://spring.io/projects/spring-boot)
- [ArchUnit Documentation](https://www.archunit.org/)
- [Azure DevOps Free Tier](https://learn.microsoft.com/en-us/azure/devops/user-guide/what-is-azure-devops)

---

## 📄 Licencia

Este proyecto es educativo y está disponible bajo MIT License.

---

## ✍️ Autores / Contribuidores

- **Creador:** LoraDev10
- **Última actualización:** 22 de Abril de 2026

---

## 💬 Preguntas / Soporte

Para dudas sobre:
- **Arquitectura:** Ver [CONCEPTOS-ARQUITECTURA.md](CONCEPTOS-ARQUITECTURA.md)
- **Tests:** Ver carpeta `src/test/`
- **CI/CD:** Ver [azure-pipelines.yml](azure-pipelines.yml)

