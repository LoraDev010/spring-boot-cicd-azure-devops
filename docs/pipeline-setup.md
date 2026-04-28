# Guía Completa: CI/CD con Azure DevOps + WSL2 + Docker

## Índice

1. [Arquitectura general](#1-arquitectura-general)
2. [Configuración en Azure DevOps](#2-configuración-en-azure-devops)
3. [Configuración de WSL2 en Windows](#3-configuración-de-wsl2-en-windows)
4. [Instalación del agente self-hosted](#4-instalación-del-agente-self-hosted)
5. [Cómo funciona el pipeline](#5-cómo-funciona-el-pipeline)
6. [Decisiones técnicas clave](#6-decisiones-técnicas-clave)
7. [Verificación y troubleshooting](#7-verificación-y-troubleshooting)

---

## 1. Arquitectura general

```
GitHub (código fuente)
        │
        │  push a main / PR
        ▼
Azure DevOps Pipelines
        │
        ├─ Stage CI ──► agente self-hosted (WSL2)
        │                   ./mvnw verify
        │                   (tests + spotless + jacoco)
        │
        └─ Stage CD ──► agente self-hosted (WSL2)  [solo en main]
                            ./mvnw package
                            docker compose build
                            docker compose up -d
                            smoke tests
```

- **Código fuente**: GitHub
- **Orquestador CI/CD**: Azure DevOps Pipelines
- **Agente de ejecución**: WSL2 (Ubuntu 24.04) corriendo en la misma máquina Windows
- **Runtime de la app**: Docker Desktop (con integración WSL2)
- **Base de datos**: PostgreSQL 16 en contenedor Docker
- **Aplicación**: Spring Boot 4 en contenedor Docker

---

## 2. Configuración en Azure DevOps

### 2.1 Crear organización y proyecto

1. Ir a [dev.azure.com](https://dev.azure.com)
2. Crear organización (si no existe)
3. Crear proyecto nuevo → nombre: `spring-boot-cicd-azure-devops`

### 2.2 Conectar repositorio GitHub

1. `Project Settings → Service connections → New service connection → GitHub`
2. Autenticar con OAuth o PAT
3. Seleccionar el repositorio

### 2.3 Crear pipeline

1. `Pipelines → New pipeline → GitHub → seleccionar repo`
2. Seleccionar "Existing Azure Pipelines YAML file"
3. Apuntar a `azure-pipelines.yml` en la raíz del repo
4. Guardar (no ejecutar aún — el agente debe estar online primero)

### 2.4 Crear pool de agentes

1. `Organization Settings → Agent pools → Add pool`
2. Tipo: **Self-hosted**
3. Nombre: `selfhost-linux`
4. Marcar "Grant access permission to all pipelines"

### 2.5 Crear environment

1. `Pipelines → Environments → New environment`
2. Nombre: `cicd-local`
3. Resource: None
4. (Opcional) Configurar aprobación manual: `cicd-local → Approvals and checks → Approvals`

### 2.6 Crear PAT para el agente

1. Click en tu avatar (arriba a la derecha) → `Personal access tokens`
2. `New Token`
   - Name: `wsl2-agent`
   - Organization: tu organización
   - Scopes: **Agent Pools → Read & manage**
3. Copiar el token (solo se muestra una vez)

---

## 3. Configuración de WSL2 en Windows

### 3.1 Instalar WSL2 + Ubuntu 24.04

```powershell
# PowerShell como Administrador
wsl --install -d Ubuntu-24.04
# Reiniciar Windows
# Configurar usuario y contraseña de Ubuntu (ej: lora / tu_password)
```

Verificar:
```powershell
wsl --list --verbose
# Debe mostrar: Ubuntu-24.04  Running  2
```

### 3.2 Evitar que WSL2 se apague solo

Crear/editar `C:\Users\<TuUsuario>\.wslconfig`:

```ini
[wsl2]
vmIdleTimeout=-1
```

Aplicar:
```powershell
wsl --shutdown
wsl
```

### 3.3 Instalar prerequisitos en Ubuntu (WSL2)

```bash
# Actualizar sistema
sudo apt-get update -y && sudo apt-get upgrade -y
sudo apt-get install -y curl git unzip wget apt-transport-https gpg

# Instalar Java 21 (Temurin)
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null

echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list

sudo apt-get update -y && sudo apt-get install -y temurin-21-jdk

# Verificar
java -version
# Debe mostrar: openjdk version "21.x.x"
```

> **Nota**: Maven no se instala globalmente. El proyecto usa `./mvnw` (Maven Wrapper) que se descarga automáticamente. El directorio `~/.m2` persiste entre ejecuciones del agente, actuando como cache nativa.

### 3.4 Integrar Docker Desktop con WSL2

1. Abrir Docker Desktop en Windows
2. `Settings → Resources → WSL Integration`
3. Activar toggle para `Ubuntu-24.04`
4. Click `Apply & Restart`

```bash
# Verificar en WSL2
docker version
docker compose version

# Si da "permission denied":
sudo usermod -aG docker $USER
# Cerrar y reabrir WSL2
```

### 3.5 Limpiar credenciales Docker en WSL2

Docker Desktop crea un config que apunta a `docker-credential-desktop.exe` (Windows), lo cual falla en el contexto del agente Linux:

```bash
# Verificar el problema
cat ~/.docker/config.json
# Si tiene "credsStore": "desktop" → fix:

echo '{}' > ~/.docker/config.json
```

---

## 4. Instalación del agente self-hosted

### 4.1 Descargar y configurar el agente

```bash
# En WSL2
mkdir -p ~/azagent && cd ~/azagent

# Obtener versión más reciente
curl -s https://api.github.com/repos/microsoft/azure-pipelines-agent/releases/latest \
  | grep '"tag_name"' | cut -d'"' -f4
# Ejemplo de resultado: v4.248.0

# Descargar (reemplazar VERSION con el número obtenido)
AGENT_VERSION="4.248.0"
curl -fSL "https://vstsagentpackage.azureedge.net/agent/${AGENT_VERSION}/vsts-agent-linux-x64-${AGENT_VERSION}.tar.gz" \
  -o agent.tar.gz
tar -xzf agent.tar.gz && rm agent.tar.gz

# Configurar (reemplazar <ORG> y <PAT>)
./config.sh \
  --url "https://dev.azure.com/<ORG>" \
  --auth pat \
  --token "<PAT>" \
  --pool "selfhost-linux" \
  --agent "wsl2-local" \
  --work "_work" \
  --acceptTeeEula \
  --unattended
```

### 4.2 Instalar como servicio systemd

```bash
cd ~/azagent

# Habilitar systemd en WSL2 (si no está activo)
# Editar /etc/wsl.conf y agregar:
# [boot]
# systemd=true
# Luego: wsl --shutdown && wsl

# Instalar servicio
sudo ./svc.sh install $USER
sudo ./svc.sh start
sudo ./svc.sh status
# Debe mostrar: active (running)

# Habilitar inicio automático
sudo systemctl enable vsts.agent.*
```

### 4.3 Verificar en Azure DevOps

`https://dev.azure.com/<ORG>/_settings/agentpools` → pool `selfhost-linux` → tab Agents → `wsl2-local` debe mostrar punto **verde (Online)**.

### 4.4 Comandos útiles del agente

```bash
# Ver estado
sudo ./svc.sh status

# Reiniciar agente
sudo ./svc.sh stop && sudo ./svc.sh start

# Ver logs del agente
ls ~/azagent/_diag/
tail -100 ~/azagent/_diag/Agent_*.log
```

---

## 5. Cómo funciona el pipeline

### 5.1 Disparadores (`azure-pipelines.yml`)

```yaml
trigger:
  branches:
    include:
      - main     # push a main → ejecuta CI + CD

pr:
  branches:
    include:
      - main     # PR hacia main → ejecuta solo CI
```

### 5.2 Stage CI — Build, Test & Verify

Se ejecuta en **cualquier push o PR** hacia `main`.

```
Checkout código (fetchDepth: 1)
    │
    ▼
chmod +x mvnw
    │
    ▼
./mvnw -B -ntp verify
    ├─ Compila el código
    ├─ Ejecuta tests unitarios (Mockito)
    ├─ Ejecuta tests de arquitectura (ArchUnit)
    ├─ Ejecuta tests de integración (H2 in-memory)
    ├─ Verifica formato (Spotless / Google Java Format)
    └─ Genera reporte de cobertura (JaCoCo)
```

**¿Por qué H2 y no PostgreSQL real en tests?**
Los tests de integración usan H2 en modo PostgreSQL (en memoria) para evitar dependencia de Docker durante CI. H2 soporta todo el SQL del schema Flyway. Ver sección 6.1.

### 5.3 Stage CD — Deploy (solo en `main`)

Condición: `and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))`

Solo corre si CI pasó **Y** el commit es en la rama `main` (no en PRs).

```
Checkout código
    │
    ▼
./mvnw -B -ntp -q -DskipTests package
    │  (tests ya corrieron en CI, solo empaqueta el JAR)
    ▼
docker compose -f deploy/compose.yaml build
    │  (construye imagen Docker de la app)
    ▼
docker compose -f deploy/compose.yaml up -d --remove-orphans
    │  (levanta postgres + app; postgres con healthcheck)
    │  (app espera a que postgres esté healthy antes de arrancar)
    ▼
bash deploy/smoke.sh
    │  (espera que /actuator/health responda 200)
    │  (crea un producto via POST /products)
    │  (lo recupera via GET /products?sku=...)
    ▼
docker compose logs (siempre, para diagnóstico)
```

### 5.4 Docker Compose (`deploy/compose.yaml`)

```yaml
postgres:
  healthcheck:
    test: pg_isready -U app -d cicd_db
    interval: 5s / retries: 10
  # La app NO arranca hasta que postgres esté healthy

app:
  depends_on:
    postgres:
      condition: service_healthy
  restart: unless-stopped   # se reinicia automáticamente si cae
```

El volumen `pgdata` persiste los datos entre deploys. La app se actualiza en cada pipeline run.

### 5.5 Flujo completo: desde push hasta deploy

```
Developer hace git push a main
        │
        ▼
GitHub notifica a Azure DevOps
        │
        ▼
Azure DevOps encola el pipeline
        │
        ▼
Agente wsl2-local (WSL2) toma el job de CI
        │  ./mvnw verify → todos los tests pasan
        ▼
Stage CD comienza (si CI pasó)
        │
        ▼
Agente wsl2-local construye JAR + imagen Docker
        │
        ▼
docker compose up reemplaza el contenedor de la app
        │  (postgres sigue corriendo con datos intactos)
        ▼
Smoke tests verifican que la API responde correctamente
        │
        ▼
Pipeline verde ✓ — nueva versión en producción local
```

---

## 6. Decisiones técnicas clave

### 6.1 H2 en lugar de TestContainers para tests de integración

**Problema original**: TestContainers usa `docker-java` que hardcodea API version 1.32. Docker Engine 29.4.1 rechaza requests con API < 1.40 → error 400 BadRequestException. Múltiples intentos de override fallaron.

**Solución**: Reemplazar TestContainers con H2 en modo PostgreSQL.

```yaml
# src/test/resources/application-test.yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
```

H2 soporta todo el SQL del migration `V1__create_products.sql`: `UUID`, `TEXT`, `VARCHAR`, `NUMERIC`, `CHAR`, `IF NOT EXISTS`, `UNIQUE`.

El test activa este perfil con `@ActiveProfiles("test")`.

### 6.2 RestClient en lugar de WebTestClient

`WebTestClient` requiere `spring-webflux`. El proyecto usa `spring-boot-starter-web` (servlet stack). Agregar webflux como dependencia de test crea conflictos en Spring Boot 4.x.

**Solución**: Usar `RestClient` (disponible en Spring 6.1+) con `@LocalServerPort` → sin dependencias adicionales.

### 6.3 Self-hosted agent en lugar de Microsoft-hosted

Los agentes Microsoft-hosted tienen límite de 1,800 minutos/mes en repos privados. El agente self-hosted en WSL2 tiene minutos ilimitados y cero costo.

Además, el agente self-hosted tiene `~/.m2` persistente → no necesita `Cache@2` task → builds más rápidos.

### 6.4 Line endings en scripts shell

Scripts `.sh` deben tener LF (Unix) no CRLF (Windows). Si Git convierte a CRLF en Windows, bash falla con `set: pipefail\r: invalid option`.

Solución en `.gitattributes`:
```
*.sh text eol=lf
/mvnw text eol=lf
```

---

## 7. Verificación y troubleshooting

### 7.1 Verificar que todo funciona

```bash
# App corriendo
curl http://localhost:8080/actuator/health
# Esperado: {"status":"UP"}

# Crear producto
curl -X POST http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"TEST-001","name":"Producto test","price":9.99,"currency":"USD"}'

# Recuperar por SKU
curl http://localhost:8080/products?sku=TEST-001

# Ver contenedores
docker ps
docker compose -f /ruta/al/repo/deploy/compose.yaml ps
```

### 7.2 Verificar el agente

```bash
# Estado del servicio
cd ~/azagent && sudo ./svc.sh status

# Logs del agente
tail -50 ~/azagent/_diag/Agent_*.log

# El agente en Azure DevOps UI
# https://dev.azure.com/<ORG>/_settings/agentpools → selfhost-linux → Agents
```

### 7.3 Tabla de errores comunes

| Error | Causa | Fix |
|-------|-------|-----|
| Pipeline queued, nunca arranca | Agente offline | `sudo ./svc.sh start` en WSL2 |
| `docker: command not found` | WSL Integration desactivada | Docker Desktop → Settings → WSL Integration → activar Ubuntu-24.04 |
| `docker-credential-desktop.exe not found` | Config Docker apunta a Windows | `echo '{}' > ~/.docker/config.json` en WSL2 |
| `set: pipefail: invalid option` | Script con CRLF | `*.sh eol=lf` en `.gitattributes` |
| `400 Bad Request` en TestContainers | docker-java API version 1.32 vs Docker Engine 29+ | Migrar a H2 (ya resuelto) |
| CD no ejecuta | Condición de rama no cumplida | Push directo a `main`, no PR |
| CD no ejecuta | Aprobación manual requerida | Azure DevOps → run → Review → Approve |
| WSL2 muere de noche | vmIdleTimeout no configurado | Agregar `vmIdleTimeout=-1` en `.wslconfig` |
| Spotless falla en pipeline | Archivos sin formatear | `./mvnw spotless:apply` + commit + push |
| `409 Conflict` en smoke tests | SKU duplicado entre runs | `smoke.sh` usa `date +%s%N` → SKU único por run |
