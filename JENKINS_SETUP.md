# Configuración de Jenkins Pipeline

Este documento describe cómo configurar Jenkins para ejecutar el pipeline de CI/CD del backend.

## 📋 Requisitos Previos

### 1. Herramientas Necesarias en Jenkins

Configura las siguientes herramientas globales en Jenkins (Manage Jenkins → Global Tool Configuration):

- **JDK 21**
  - Nombre: `JDK-21`
  - Instalación automática desde adoptium.net o ruta personalizada

- **Maven 3.9+**
  - Nombre: `Maven-3.9`
  - Versión: 3.9.x o superior
  - Instalación automática o ruta personalizada

### 2. Plugins Requeridos

Instala los siguientes plugins en Jenkins:

```
- Pipeline (plugin básico)
- Docker Pipeline
- Docker Plugin
- AnsiColor
- Timestamper
- HTML Publisher Plugin
- JUnit Plugin
- JaCoCo Plugin
- SonarQube Scanner (opcional, si usas SonarQube)
- OWASP Dependency-Check Plugin (opcional)
- Email Extension Plugin (opcional, para notificaciones)
- Build Timestamp Plugin
```

### 3. Credenciales Necesarias

Configura las siguientes credenciales en Jenkins (Manage Jenkins → Credentials):

1. **docker-registry-credentials**
   - Tipo: Username with password
   - Usuario y contraseña del registro Docker (Docker Hub, Azure Container Registry, etc.)

2. **sonar-token** (opcional)
   - Tipo: Secret text
   - Token de SonarQube

3. **azure-credentials** (opcional, si usas Azure)
   - Tipo: Secret text o Service Principal

4. **docker-registry-url** (opcional)
   - Tipo: Secret text
   - URL del registro Docker privado

## 🔧 Configuración del Pipeline

### Opción 1: Pipeline desde SCM (Recomendado)

1. Crea un nuevo **Pipeline** job en Jenkins
2. En la configuración:
   - **Definition**: Pipeline script from SCM
   - **SCM**: Git
   - **Repository URL**: URL de tu repositorio Git
   - **Credentials**: Credenciales de Git si es necesario
   - **Branch Specifier**: `*/main` o la rama que uses
   - **Script Path**: `back/Jenkinsfile`

### Opción 2: Pipeline Multibranch

1. Crea un **Multibranch Pipeline** job
2. Configura el SCM y Jenkins detectará automáticamente el `Jenkinsfile` en cada rama

## 🌍 Variables de Entorno

Si necesitas configurar variables de entorno personalizadas, puedes hacerlo:

1. En el job: **Configure → Environment variables**
2. O modificar la sección `environment` en el `Jenkinsfile`

Variables importantes:
- `SONAR_HOST_URL`: URL de tu servidor SonarQube (default: http://localhost:9000)
- `DOCKER_REGISTRY`: URL del registro Docker
- `DB_HOST_TEST`, `DB_PORT_TEST`, etc.: Configuración de base de datos para tests

## 🐳 Requisitos de Docker

El pipeline requiere que Docker esté instalado y accesible en el agente de Jenkins:

```bash
# Verificar que Docker está disponible
docker --version

# Verificar permisos
docker ps

# Si hay problemas de permisos, agregar el usuario de Jenkins al grupo docker:
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

## 📊 Configuración de SonarQube (Opcional)

1. Instala y configura SonarQube Server
2. En Jenkins: **Manage Jenkins → Configure System → SonarQube servers**
3. Agrega tu servidor SonarQube:
   - Name: `SonarQube`
   - Server URL: URL de tu SonarQube
   - Server authentication token: Token generado en SonarQube

## 🚀 Ejecución del Pipeline

### Parámetros del Pipeline

El pipeline acepta los siguientes parámetros:

- **DEPLOY_ENV**: Ambiente de despliegue (`none`, `staging`, `production`)
- **SKIP_TESTS**: Omitir tests (no recomendado)
- **SKIP_SONAR**: Omitir análisis de SonarQube
- **BUILD_DOCKER**: Construir imagen Docker (default: true)
- **PUSH_DOCKER**: Subir imagen al registro (default: false)

### Ejecutar Manualmente

1. Abre el job en Jenkins
2. Click en **Build with Parameters**
3. Configura los parámetros según necesites
4. Click en **Build**

## 📈 Monitoreo y Reportes

El pipeline genera varios reportes:

- **JUnit Test Results**: Resultados de tests unitarios
- **JaCoCo Coverage**: Reporte de cobertura de código
- **OWASP Dependency Check**: Análisis de vulnerabilidades
- **SonarQube**: Análisis de calidad de código (si está configurado)
- **Build Info**: Información general del build

Todos los reportes están disponibles en la página del build.

## 🔍 Solución de Problemas

### Error: Tool 'JDK-21' not found
- Configura JDK en **Manage Jenkins → Global Tool Configuration**

### Error: Docker daemon not running
- Inicia Docker: `sudo systemctl start docker`
- Verifica permisos del usuario Jenkins

### Error: Maven dependencies download fails
- Verifica conectividad a internet
- Revisa configuración de proxy si es necesario
- El pipeline usa caché de Maven para mejorar rendimiento

### Error: PostgreSQL test container fails
- Verifica que el puerto 5437 esté disponible
- Verifica permisos de Docker para crear contenedores
- Revisa logs: `docker logs test-postgres`

### Tests fallan
- Revisa logs de tests en `target/surefire-reports/`
- Verifica configuración de base de datos de pruebas
- Asegúrate de que el contenedor de PostgreSQL esté funcionando

### SonarQube no se ejecuta
- Verifica que el servidor SonarQube esté configurado
- Revisa que las credenciales sean correctas
- Puedes omitir SonarQube con el parámetro `SKIP_SONAR=true`

## 🔐 Seguridad

### Mejores Prácticas

1. **Nunca commitees credenciales** en el código o Jenkinsfile
2. **Usa Jenkins Credentials** para secretos
3. **Restringe acceso** al pipeline según roles
4. **Habilita audit logs** en Jenkins
5. **Usa HTTPS** para comunicación con registros Docker
6. **Actualiza plugins** regularmente

### Rotación de Credenciales

- Rota credenciales regularmente
- Usa tokens de corta duración cuando sea posible
- Monitorea accesos no autorizados

## 📚 Recursos Adicionales

- [Documentación de Jenkins Pipeline](https://www.jenkins.io/doc/book/pipeline/)
- [Documentación de Docker](https://docs.docker.com/)
- [Documentación de SonarQube](https://docs.sonarqube.org/)
- [Best Practices for Jenkins](https://www.jenkins.io/doc/book/using/using-jenkins-best-practices/)

## 💡 Optimizaciones de Rendimiento

El pipeline ya incluye optimizaciones:

- ✅ Caché de dependencias Maven entre builds
- ✅ Ejecución paralela de tests y BD
- ✅ Builds incrementales cuando es posible
- ✅ Uso de multi-stage Docker builds
- ✅ Timeouts para evitar builds colgados

### Mejoras Adicionales Recomendadas

1. **Usar agentes dedicados** para builds
2. **Implementar build cache** para dependencias
3. **Usar BuildKit** de Docker para builds más rápidos
4. **Paralelizar tests** a nivel de suite si es posible
5. **Usar agentes con más recursos** para builds grandes

## 📞 Soporte

Si tienes problemas con el pipeline:

1. Revisa los logs del build en Jenkins
2. Verifica la configuración según esta guía
3. Consulta los reportes generados
4. Contacta al equipo de DevOps

---

**Última actualización**: $(date +%Y-%m-%d)
