# Documentación del Pipeline Jenkins - Marketplace Link Backend

## Descripción General

Este pipeline de Jenkins automatiza el proceso completo de integración y despliegue continuo (CI/CD) para el backend del proyecto Marketplace Link. Incluye construcción de imágenes Docker, validación local con Docker Compose, ejecución de tests automatizados con Postman/Newman, y despliegue opcional a Azure.

---

## Configuración del Pipeline

### Opciones Globales

```groovy
options {
    timestamps()                                    // Marca de tiempo en los logs
    ansiColor('xterm')                             // Colores en la consola
    timeout(time: 45, unit: 'MINUTES')             // Timeout de 45 minutos
    buildDiscarder(logRotator(numToKeepStr: '10')) // Mantener solo 10 builds
}
```

### Variables de Entorno

- **DOCKER_IMAGE**: `marketplace-link-backend` - Nombre de la imagen Docker
- **DOCKER_TAG**: Número del build actual (auto-incremental)
- **POSTMAN_BASE_URL**: URL del servidor para tests (default: `http://localhost:8080`)
- **POSTMAN_USER_EMAIL**: Email de usuario para tests (default: `test@example.com`)
- **POSTMAN_USER_PASSWORD**: Contraseña para tests (default: `password123`)

### Parámetros Configurables

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `BUILD_DOCKER` | Boolean | `true` | Construir imagen Docker |
| `PUSH_DOCKER` | Boolean | `false` | Subir imagen al registro |
| `TEST_LOCAL_DOCKER` | Boolean | `false` | Validar con Docker Compose local |
| `DEPLOY_ENV` | Choice | `none` | Entorno de despliegue (none/staging/production) |

---

## Stages del Pipeline

### 1. Checkout

**Propósito**: Clonar el código del repositorio y detectar la estructura del proyecto.

**Funcionalidades**:
- Clona el código desde Git
- Obtiene el hash corto del commit (`GIT_COMMIT_SHORT`)
- **Detección automática de estructura**:
  - Si `pom.xml` y `Dockerfile` están en la raíz → workspace es `/back`
  - Si están en `/back` → workspace es raíz del repositorio
- Establece la variable `PROJECT_DIR` para etapas posteriores

**Salida**:
```
✅ Detectado: workspace es el directorio back/
Commit: a1b2c3d
Directorio del proyecto: .
```

---

### 2. Validación de Proyecto

**Propósito**: Verificar que existen los archivos necesarios para construir la aplicación.

**Validaciones**:
- ✅ Existe `pom.xml` (proyecto Maven)
- ✅ Existe `Dockerfile` (configuración Docker)

**Resultado**:
```
✅ Validación OK: pom.xml y Dockerfile encontrados en ./
```

---

### 3. Construir Imagen Docker

**Condición**: Solo si `BUILD_DOCKER = true`

**Proceso**:
1. Genera metadatos del build:
   - Fecha y hora UTC
   - Hash del commit Git
   - Número de versión (BUILD_NUMBER)

2. Construye la imagen Docker con:
   ```bash
   docker build \
       --build-arg BUILD_DATE="2025-11-23" \
       --build-arg BUILD_TIME="15:30:00" \
       --build-arg GIT_COMMIT="a1b2c3d" \
       --build-arg VERSION="36" \
       -t marketplace-link-backend:36 \
       -t marketplace-link-backend:latest \
       .
   ```

3. Genera **dos tags**:
   - `marketplace-link-backend:<BUILD_NUMBER>` - Versión específica
   - `marketplace-link-backend:latest` - Versión actual

**Resultado**:
```
✅ Imagen Docker construida: marketplace-link-backend:36
```

---

### 4. Validación Local (Docker Compose)

**Condición**: Solo si `TEST_LOCAL_DOCKER = true` y `BUILD_DOCKER = true`

**Propósito**: Validar que la aplicación funciona correctamente en un entorno local usando Docker Compose antes de ejecutar tests.

#### 4.1. Limpieza de Contenedores

Elimina contenedores previos para evitar conflictos de puertos:

```bash
# Parar servicios con docker-compose
docker compose down -v

# Forzar eliminación de contenedores antiguos (snake_case)
docker stop mplink_backend mplink_postgres...
docker rm mplink_backend mplink_postgres...

# Forzar eliminación de contenedores nuevos (kebab-case)
docker stop mplink-backend mplink-postgres...
docker rm mplink-backend mplink-postgres...
```

**Razón**: Evita el error `Bind for 0.0.0.0:5436 failed: port is already allocated`

#### 4.2. Inicio de Servicios

```bash
docker-compose up -d mplink-postgres mplink-postgres-test mplink-backend
```

**Servicios levantados**:
- `mplink-postgres`: Base de datos principal (puerto 5436)
- `mplink-postgres-test`: Base de datos de pruebas (puerto 5437)
- `mplink-backend`: Aplicación Spring Boot (puerto 8080)

#### 4.3. Healthcheck de Base de Datos

Espera hasta 60 segundos a que PostgreSQL esté listo:

```bash
pg_isready -U postgres -d marketplace_db
```

**Salida**:
```
✅ Base de datos lista
```

#### 4.4. Healthcheck del Backend

Espera hasta 180 segundos verificando:

1. **Estado del contenedor Docker**:
   ```bash
   docker inspect --format='{{.State.Health.Status}}' mplink-backend
   ```

2. **Respuesta del endpoint de salud**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

**Criterios de éxito**:
- Estado Docker: `healthy`
- HTTP Status Code: 200-499 (no 5xx)

**Salida**:
```
✅ Backend está healthy según Docker
✅ Backend responde (HTTP 200)
✅ Validación local completada exitosamente
```

---

### 5. Tests (Postman/Newman)

**Condición**: Si `BUILD_DOCKER = true` o hay una URL remota configurada

**Propósito**: Ejecutar tests de API automatizados usando las colecciones Postman del proyecto.

#### 5.1. Detección de Colecciones

Busca archivos JSON en el directorio `tests/`:

```bash
find tests -maxdepth 1 -name '*.json' -o -name '*.postman_collection.json'
```

**Ejemplo de salida**:
```
📋 Encontradas 1 colección(es) Postman
   - tests/publications.postman_collection.json
```

#### 5.2. Configuración de Red Docker

**Caso 1: Modo Local (`TEST_LOCAL_DOCKER = true`)**
- Detecta la red Docker del contenedor `mplink-backend`
- Cambia BASE_URL a `http://mplink-backend:8080` (resolución DNS interna)
- Ejecuta Newman en la misma red Docker

**Caso 2: Modo Remoto**
- Usa la URL configurada (ej: `https://staging.example.com`)
- Newman se conecta directamente sin red Docker

**Detección automática de red**:
```bash
# Obtener ID de red del contenedor
docker inspect mplink-backend | grep NetworkID

# Obtener nombre de la red
docker network inspect <network_id> --format '{{.Name}}'
```

**Resultado**:
```
🔍 Red detectada: back_mplink_net
🔄 Cambiando BASE_URL a: http://mplink-backend:8080 (nombre del contenedor)
```

#### 5.3. Pre-validación del Backend

Antes de ejecutar tests, verifica:

1. **Spring Boot completamente iniciado**:
   ```bash
   docker logs mplink-backend | grep "Started BackApplication"
   ```

2. **Prueba del endpoint de login**:
   ```bash
   curl -X POST http://mplink-backend:8080/api/auth/login \
        -H 'Content-Type: application/json' \
        -d '{"email":"test@example.com","password":"password123"}'
   ```

3. **Verificación de logs JWT**:
   ```bash
   docker logs mplink-backend | grep "JWT-LOGIN"
   ```

**Salida esperada**:
```
✅ Spring Boot ha arrancado completamente
🔍 Probando endpoint de login directamente...
HTTP_CODE:200
🔍 Verificando logs del backend después del intento de login...
[JWT-LOGIN] ========== INICIO attemptAuthentication ==========
```

#### 5.4. Ejecución de Tests con Newman

Para cada colección encontrada:

```bash
docker run --rm --network back_mplink_net \
    -v "$(pwd):/workspace" \
    -w /workspace \
    -e BASE_URL="http://mplink-backend:8080" \
    -e USER_EMAIL="test@example.com" \
    -e USER_PASSWORD="password123" \
    postman/newman:latest \
    run "tests/publications.postman_collection.json" \
    --env-var "BASE_URL=http://mplink-backend:8080" \
    --env-var "USER_EMAIL=test@example.com" \
    --env-var "USER_PASSWORD=password123" \
    --reporters cli,junit \
    --reporter-junit-export "target/newman-publications.xml"
```

**Características**:
- Ejecuta en contenedor Docker aislado
- Comparte la red con el backend (si es local)
- Genera reportes JUnit XML para Jenkins
- Muestra resultado en consola

**Salida**:
```
🚀 Ejecutando tests con Docker (postman/newman:latest)...
┌─────────────────────────┬──────────┬──────────┐
│                         │ executed │   failed │
├─────────────────────────┼──────────┼──────────┤
│              iterations │        1 │        0 │
├─────────────────────────┼──────────┼──────────┤
│                requests │        3 │        0 │
├─────────────────────────┼──────────┼──────────┤
│            test-scripts │        3 │        0 │
├─────────────────────────┼──────────┼──────────┤
│      prerequest-scripts │        1 │        0 │
├─────────────────────────┼──────────┼──────────┤
│              assertions │        5 │        0 │
└─────────────────────────┴──────────┴──────────┘
✅ Colección tests/publications.postman_collection.json ejecutada
```

#### 5.5. Publicación de Resultados

**Post-actions (siempre se ejecutan)**:

1. Busca archivos XML generados:
   ```bash
   find target -name '*.xml' -type f
   ```

2. Publica resultados en Jenkins:
   ```groovy
   junit 'target/*.xml'
   ```

**Resultado en Jenkins**:
- ✅ Tests passed: 5/5
- 📊 Gráficos de tendencia histórica
- 🔍 Detalles de cada test individual

---

### 6. Push Imagen

**Condición**: Solo si `PUSH_DOCKER = true` y `BUILD_DOCKER = true`

**Propósito**: Subir la imagen Docker construida a un registro (Docker Hub, Azure Container Registry, etc.).

**Proceso**:
```groovy
withDockerRegistry([credentialsId: 'docker-registry-credentials']) {
    sh "docker push marketplace-link-backend:36"
    sh "docker push marketplace-link-backend:latest"
}
```

**Requisito previo**:
- Configurar credenciales `docker-registry-credentials` en Jenkins

---

### 7. Deploy to Azure

**Condición**: Solo si `DEPLOY_ENV != 'none'` y `BUILD_DOCKER = true`

**Propósito**: Desplegar la aplicación a Azure Container Apps.

**Proceso**:
```bash
# Login a Azure con Service Principal
az login --service-principal \
    -u $AZURE_CLIENT_ID \
    -p $AZURE_CLIENT_SECRET \
    --tenant $AZURE_TENANT_ID

# Actualizar Container App con nueva imagen
az containerapp update \
    --name marketplace-link-backend \
    --resource-group mi-grupo \
    --image marketplace-link-backend:36
```

**Requisito previo**:
- Configurar credenciales Azure `azure-credentials-id` en Jenkins

---

### 8. Limpiar Contenedores de Prueba

**Condición**: Solo si `TEST_LOCAL_DOCKER = true` y `BUILD_DOCKER = true`

**Propósito**: Liberar recursos eliminando los contenedores de prueba.

**Proceso**:
```bash
docker-compose down -v          # Detener y eliminar contenedores
docker image prune -f           # Limpiar imágenes dangling
```

**Resultado**:
```
🧹 Limpiando contenedores de prueba...
✅ Contenedores eliminados correctamente
```

---

## Flujo de Ejecución Completo

### Escenario 1: Build Local con Tests (Modo Desarrollo)

```
Parámetros:
- BUILD_DOCKER = true
- TEST_LOCAL_DOCKER = true
- PUSH_DOCKER = false
- DEPLOY_ENV = none

Flujo:
1. Checkout → ✅
2. Validación → ✅
3. Build Docker → ✅ (imagen:36 creada)
4. Validación Local → ✅ (docker-compose up)
5. Tests Postman → ✅ (5/5 tests passed)
6. Push → ⏭️ (skip)
7. Deploy → ⏭️ (skip)
8. Cleanup → ✅ (contenedores eliminados)

Duración estimada: 8-12 minutos
```

### Escenario 2: Build y Deploy a Staging

```
Parámetros:
- BUILD_DOCKER = true
- TEST_LOCAL_DOCKER = false
- PUSH_DOCKER = true
- DEPLOY_ENV = staging

Flujo:
1. Checkout → ✅
2. Validación → ✅
3. Build Docker → ✅
4. Validación Local → ⏭️ (skip)
5. Tests Postman → ✅ (contra URL remota)
6. Push → ✅ (imagen subida a registry)
7. Deploy → ✅ (desplegado a Azure)
8. Cleanup → ⏭️ (skip)

Duración estimada: 10-15 minutos
```

### Escenario 3: Solo Tests contra Producción

```
Parámetros:
- BUILD_DOCKER = false
- POSTMAN_BASE_URL = https://api.production.com
- DEPLOY_ENV = none

Flujo:
1. Checkout → ✅
2. Validación → ✅
3. Build Docker → ⏭️ (skip)
4. Validación Local → ⏭️ (skip)
5. Tests Postman → ✅ (contra producción)
6. Push → ⏭️ (skip)
7. Deploy → ⏭️ (skip)
8. Cleanup → ⏭️ (skip)

Duración estimada: 2-3 minutos
```

---

## Manejo de Errores

### 1. Error de Puerto Ocupado

**Problema**:
```
Error: Bind for 0.0.0.0:5436 failed: port is already allocated
```

**Solución implementada**:
- Stage "Validación Local" limpia **todos** los contenedores (viejos y nuevos)
- Verifica que no queden contenedores huérfanos antes de iniciar

### 2. Backend No Inicia

**Problema**:
```
❌ Timeout esperando el backend
```

**Diagnóstico automático**:
```bash
docker ps -a | grep mplink              # Estado de contenedores
docker logs mplink-backend              # Logs completos
docker exec mplink-backend ps aux       # Procesos internos
```

### 3. Tests Fallan

**Problema**:
```
Newman run failed with errors
```

**Información proporcionada**:
- Logs completos del backend
- Respuesta HTTP del endpoint de login
- Estado de red Docker
- Variables de entorno utilizadas

---

## Configuración Requerida en Jenkins

### Credenciales

1. **Docker Registry** (ID: `docker-registry-credentials`):
   - Tipo: Username with password
   - Uso: Push de imágenes Docker

2. **Azure Service Principal** (ID: `azure-credentials-id`):
   - Tipo: Azure Service Principal
   - Uso: Despliegue a Azure
   - Permisos requeridos: Contributor en Resource Group

### Plugins Necesarios

- Docker Pipeline
- JUnit
- ANSI Color
- Azure Credentials
- Workspace Cleanup

### Variables de Entorno Globales (Opcionales)

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `POSTMAN_BASE_URL` | URL por defecto para tests | `http://localhost:8080` |
| `POSTMAN_USER_EMAIL` | Usuario de test | `test@example.com` |
| `POSTMAN_USER_PASSWORD` | Contraseña de test | `password123` |

---

## Métricas y Monitoreo

### Duración Típica de Stages

| Stage | Duración | Variabilidad |
|-------|----------|--------------|
| Checkout | ~10s | Baja |
| Validación | ~2s | Muy Baja |
| Build Docker | 3-5min | Media (según caché) |
| Validación Local | 1-3min | Media |
| Tests Postman | 30s-2min | Alta (según tests) |
| Push Imagen | 30s-1min | Media (según tamaño) |
| Deploy Azure | 2-4min | Alta |
| Cleanup | ~5s | Muy Baja |

### Indicadores de Éxito

- ✅ **Build Success Rate**: >95%
- ✅ **Test Pass Rate**: 100% (bloqueante)
- ✅ **Deployment Success**: >98%
- ✅ **Avg Build Time**: <10min

---

## Mejores Prácticas Implementadas

1. **Detección automática de estructura**: Funciona con diferentes layouts de repositorio
2. **Cleanup exhaustivo**: Evita conflictos de puertos y recursos
3. **Healthchecks robustos**: Verifica estado real antes de continuar
4. **Logging detallado**: Facilita debugging en caso de errores
5. **Ejecución aislada**: Tests en contenedores Docker independientes
6. **Reportes estándar**: Formato JUnit compatible con Jenkins
7. **Rollback seguro**: No afecta producción si los tests fallan

---

## Comandos Útiles para Debugging Local

```bash
# Ver logs del backend
docker logs mplink-backend -f

# Verificar estado de contenedores
docker ps -a | grep mplink

# Probar endpoint manualmente
curl -X POST http://localhost:8080/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"test@example.com","password":"password123"}'

# Ver redes Docker
docker network ls | grep mplink

# Limpiar todo manualmente
docker-compose down -v
docker rm -f $(docker ps -a | grep mplink | awk '{print $1}')
```

---

## Diagrama de Flujo del Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                         INICIO PIPELINE                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   1. Checkout   │
                    │   - Clone repo  │
                    │   - Detect dir  │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  2. Validación  │
                    │  - Check pom.xml│
                    │  - Check Docker │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │ BUILD_DOCKER = true?        │
              └──────┬──────────────┬───────┘
                 NO  │              │ YES
                     │     ┌────────▼────────┐
                     │     │ 3. Build Docker │
                     │     │ - Compile Maven │
                     │     │ - Create image  │
                     │     └────────┬────────┘
                     │              │
                     │    ┌─────────┴─────────────┐
                     │    │ TEST_LOCAL_DOCKER?    │
                     │    └─────┬───────────┬─────┘
                     │      NO  │           │ YES
                     │          │  ┌────────▼─────────┐
                     │          │  │ 4. Validación    │
                     │          │  │    Local         │
                     │          │  │ - docker-compose │
                     │          │  │ - healthchecks   │
                     │          │  └────────┬─────────┘
                     │          │           │
                     └──────────┴───────────┘
                                │
                       ┌────────▼────────┐
                       │  5. Tests       │
                       │     Postman     │
                       │  - Newman run   │
                       │  - JUnit report │
                       └────────┬────────┘
                                │
                  ┌─────────────┴─────────────┐
                  │ PUSH_DOCKER = true?       │
                  └─────┬─────────────┬───────┘
                    NO  │             │ YES
                        │   ┌─────────▼────────┐
                        │   │ 6. Push Imagen   │
                        │   │ - docker push    │
                        │   └─────────┬────────┘
                        │             │
                        └─────────────┘
                                │
                  ┌─────────────┴──────────────┐
                  │ DEPLOY_ENV != 'none'?      │
                  └─────┬──────────────┬───────┘
                    NO  │              │ YES
                        │   ┌──────────▼────────┐
                        │   │ 7. Deploy Azure   │
                        │   │ - az login        │
                        │   │ - az update       │
                        │   └──────────┬────────┘
                        │              │
                        └──────────────┘
                                │
                  ┌─────────────┴─────────────┐
                  │ TEST_LOCAL_DOCKER = true? │
                  └─────┬─────────────┬───────┘
                    NO  │             │ YES
                        │   ┌─────────▼────────┐
                        │   │ 8. Cleanup       │
                        │   │ - docker down    │
                        │   └─────────┬────────┘
                        │             │
                        └─────────────┘
                                │
                    ┌───────────▼───────────┐
                    │     FIN PIPELINE      │
                    │   Build: SUCCESS      │
                    └───────────────────────┘
```

---

## Historial de Cambios

### Versión 2.0 (2025-11-24)
- ✅ Renombrado de servicios Docker a kebab-case (mplink-backend)
- ✅ Limpieza exhaustiva de contenedores huérfanos
- ✅ Verificación de logs JWT antes de tests
- ✅ Detección automática de red Docker
- ✅ Pre-validación del endpoint de login

### Versión 1.0 (2025-11-20)
- ✅ Implementación inicial del pipeline
- ✅ Integración con Docker Compose
- ✅ Tests automatizados con Newman
- ✅ Despliegue a Azure Container Apps

---

## Contacto y Soporte

Para reportar problemas o sugerencias sobre el pipeline:

- **Repositorio**: [https://github.com/DRTX2/Marketplace_Link](https://github.com/DRTX2/Marketplace_Link)
- **Documentación adicional**: Ver carpeta `/docs`
- **Issues**: GitHub Issues del repositorio
