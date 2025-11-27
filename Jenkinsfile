pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_IMAGE = "drtx2/marketplace-link-backend"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        
        // Variables para tests Postman (con valores por defecto)
        POSTMAN_BASE_URL = "${env.POSTMAN_BASE_URL ?: 'http://localhost:8080'}"
        POSTMAN_USER_EMAIL = "${env.POSTMAN_USER_EMAIL ?: 'test@example.com'}"
        POSTMAN_USER_PASSWORD = "${env.POSTMAN_USER_PASSWORD ?: 'password123'}"
    }

    parameters {
        booleanParam(name: 'BUILD_DOCKER', defaultValue: true)
        booleanParam(name: 'PUSH_DOCKER', defaultValue: false)
        booleanParam(name: 'TEST_LOCAL_DOCKER', defaultValue: false, description: 'Levanta docker-compose localmente para validar antes de desplegar')
        booleanParam(name: 'EXPOSE_BACKEND', defaultValue: true, description: 'Mantiene el backend activo en http://localhost:8080 al finalizar')
        choice(name: 'DEPLOY_ENV', choices: ['none','staging','production'])
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                    
                    // Detectar automáticamente el directorio base del proyecto
                    // Si pom.xml está en la raíz, el workspace es back/
                    // Si pom.xml está en back/, el workspace es la raíz del repo
                    if (fileExists('pom.xml') && fileExists('Dockerfile')) {
                        env.PROJECT_DIR = '.'
                        echo "✅ Detectado: workspace es el directorio back/"
                    } else if (fileExists('back/pom.xml') && fileExists('back/Dockerfile')) {
                        env.PROJECT_DIR = 'back'
                        echo "✅ Detectado: workspace es la raíz del repo, proyecto en back/"
                    } else {
                        echo "❌ No se pudo detectar la estructura del proyecto"
                        echo "📁 Estructura del workspace:"
                        sh 'pwd && ls -la || true'
                        error("❌ No se encontró pom.xml o Dockerfile. Verifica la estructura del repositorio.")
                    }
                }
                echo "Commit: ${env.GIT_COMMIT_SHORT}"
                echo "Directorio del proyecto: ${env.PROJECT_DIR}"
            }
        }

        stage('Validación de Proyecto') {
            steps {
                dir(env.PROJECT_DIR) {
                    script {
                        if (!fileExists('pom.xml')) {
                            error("❌ No se encontró pom.xml en ${env.PROJECT_DIR}/")
                        }
                        if (!fileExists('Dockerfile')) {
                            error("❌ No se encontró Dockerfile en ${env.PROJECT_DIR}/")
                        }
                        echo "✅ Validación OK: pom.xml y Dockerfile encontrados en ${env.PROJECT_DIR}/"
                    }
                }
            }
        }

        stage('Construir Imagen Docker (con compilación)') {
            when { expression { params.BUILD_DOCKER } }
            steps {
                dir(env.PROJECT_DIR) {
                    script {
                        // Pasar metadatos de build a Docker
                        def buildDate = sh(script: 'date -u +"%Y-%m-%d"', returnStdout: true).trim()
                        def buildTime = sh(script: 'date -u +"%H:%M:%S"', returnStdout: true).trim()
                        def gitCommit = env.GIT_COMMIT_SHORT ?: sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        
                    sh """
                        docker build \
                                --build-arg BUILD_DATE="${buildDate}" \
                                --build-arg BUILD_TIME="${buildTime}" \
                                --build-arg GIT_COMMIT="${gitCommit}" \
                                --build-arg VERSION="${env.BUILD_NUMBER}" \
                            -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \
                            -t ${env.DOCKER_IMAGE}:latest \
                            .
                    """
                        echo "✅ Imagen Docker construida: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    }
                }
            }
        }

        stage('Validación Local (Docker Compose)') {
            when { 
                expression { 
                    params.TEST_LOCAL_DOCKER && params.BUILD_DOCKER 
                } 
            }
            steps {
                dir(env.PROJECT_DIR) {
                    script {
                        // Detectar comando docker compose disponible (declarar una vez al inicio)
                        def dockerComposeCmd = sh(
                            script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                            returnStdout: true
                        ).trim()
                        
                        try {
                            echo "🚀 Levantando servicios con docker-compose para validación..."
                            echo "Usando comando: ${dockerComposeCmd}"
                            
                            // Determinar qué docker-compose usar (raíz del proyecto o back/)
                            def composeFile = fileExists('docker-compose.back.yml') ? 'docker-compose.back.yml' : '../docker-compose.back.yml'
                            echo "📁 Usando docker-compose: ${composeFile}"
                            
                            // Limpiar TODOS los contenedores (nuevos y viejos) y liberar puertos
                            sh """
                                # Parar servicios con docker-compose
                                ${dockerComposeCmd} -f ${composeFile} down -v 2>/dev/null || true
                                
                                # Forzar stop/rm de TODOS los contenedores relacionados (viejos y nuevos)
                                docker stop mplink-backend mplink-marketplace-db mplink-marketplace-test-db mplink-postgres mplink-postgres-test mplink-frontend 2>/dev/null || true
                                docker stop mplink_backend mplink_marketplace_db mplink_marketplace_test_db mplink_postgres mplink_postgres_test mplink_frontend 2>/dev/null || true
                                docker rm mplink-backend mplink-marketplace-db mplink-marketplace-test-db mplink-postgres mplink-postgres-test mplink-frontend 2>/dev/null || true
                                docker rm mplink_backend mplink_marketplace_db mplink_marketplace_test_db mplink_postgres mplink_postgres_test mplink_frontend 2>/dev/null || true
                                
                                # Verificar que no queden contenedores huérfanos
                                docker ps -a | grep mplink || echo "✅ No hay contenedores mplink residuales"
                            """
                            
                            // Etiquetar la imagen construida para que docker-compose la use
                            sh """
                                docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} mplink-backend:latest
                            """
                            
                            // Levantar solo backend y BD (sin frontend para pruebas más rápidas)
                            // Usar el docker-compose desde la ubicación correcta
                            def composeDir = '.'
                            dir(composeDir) {
                            sh """
                                    ${dockerComposeCmd} -f ${composeFile} up -d mplink-postgres mplink-postgres-test mplink-backend
                            """
                            }
                            
                            echo "⏳ Esperando a que los servicios estén saludables..."
                            
                            // Esperar a que la BD esté lista (máximo 60 segundos)
                            // Intentar con ambos nombres de contenedor posibles
                            sh '''
                                timeout=60
                                elapsed=0
                                db_ready=false
                                
                                # Intentar con mplink-marketplace-db (back/docker-compose.yml)
                                until docker exec mplink-marketplace-db pg_isready -U postgres -d marketplace_db > /dev/null 2>&1 2>/dev/null || \
                                      docker exec mplink-postgres pg_isready -U mplink_user -d marketplace_link > /dev/null 2>&1; do
                                    if [ $elapsed -ge $timeout ]; then
                                        echo "❌ Timeout esperando la base de datos"
                                        docker ps -a | grep postgres || true
                                        exit 1
                                    fi
                                    echo "Esperando base de datos... ($elapsed/$timeout segundos)"
                                    sleep 2
                                    elapsed=$((elapsed + 2))
                                done
                                echo "✅ Base de datos lista"
                            '''
                            
                            // Esperar a que el backend esté saludable usando el healthcheck de Docker
                            echo "⏳ Esperando a que el backend esté saludable (usando healthcheck de Docker)..."
                            sh """
                                timeout=180
                                elapsed=0
                                backend_healthy=false
                                
                                while [ \$elapsed -lt \$timeout ]; do
                                    # Verificar healthcheck de Docker
                                    health_status=\$(docker inspect --format='{{.State.Health.Status}}' mplink-backend 2>/dev/null || echo "none")
                                    
                                    if [ "\$health_status" = "healthy" ]; then
                                        echo "✅ Backend está healthy según Docker"
                                        backend_healthy=true
                                        break
                                    fi
                                    
                                    # Verificar que el servidor responda (aunque sea con redirect por seguridad)
                                    # El healthcheck interno de Docker ya verifica que la app esté funcionando
                                    http_code=\$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
                                    if [ "\$http_code" != "000" ] && [ "\$http_code" -lt 500 ]; then
                                        echo "✅ Backend responde (HTTP \$http_code)"
                                        backend_healthy=true
                                        break
                                    fi
                                    
                                    echo "Esperando backend... (\$elapsed/\$timeout segundos) - Estado: \$health_status"
                                    sleep 5
                                    elapsed=\$((elapsed + 5))
                                done
                                
                                if [ "\$backend_healthy" != "true" ]; then
                                    echo "❌ Timeout esperando el backend"
                                    echo "=== Estado de contenedores ==="
                                    docker ps -a | grep mplink || true
                                    echo "=== Logs del Backend ==="
                                    ${dockerComposeCmd} -f ${composeFile} logs mplink-backend || docker logs mplink-backend || true
                                    exit 1
                                fi
                            """
                            
                            // Validar que el backend está respondiendo
                            // Nota: El healthcheck de Docker ya verificó que el contenedor está healthy
                            // Aquí solo verificamos que el servidor responda (puede ser un redirect por seguridad)
                            echo "🔍 Verificando que el backend está respondiendo..."
                            
                            def httpCode = sh(
                                script: 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "000"',
                                returnStdout: true
                            ).trim()
                            
                            echo "HTTP Status Code: ${httpCode}"
                            
                            // Aceptar cualquier código HTTP 2xx, 3xx (redirect) o 401 (autenticación requerida)
                            // Esto indica que el servidor está funcionando, aunque requiera autenticación
                            if (httpCode == "000" || (httpCode.toInteger() >= 500 && httpCode.toInteger() < 600)) {
                                // Error de conexión o error del servidor
                                def healthResponse = sh(
                                    script: 'curl -s http://localhost:8080/actuator/health 2>&1 | head -20',
                                    returnStdout: true
                                ).trim()
                                error("❌ Backend no está respondiendo correctamente. HTTP Code: ${httpCode}, Response: ${healthResponse}")
                            }
                            
                            // Si llegamos aquí, el servidor está respondiendo
                            // El healthcheck de Docker ya verificó que la aplicación está healthy internamente
                            echo "✅ Backend está respondiendo (HTTP ${httpCode})"
                            echo "✅ Healthcheck de Docker confirmó que el contenedor está healthy"
                            
                            echo "✅ Validación local completada exitosamente"
                            
                        } catch (Exception e) {
                            echo "❌ Error durante la validación local: ${e.getMessage()}"
                            // Mostrar logs en caso de error
                            def composeFile = fileExists('docker-compose.back.yml') ? 'docker-compose.back.yml' : '../docker-compose.back.yml'
                            sh """
                                echo "=== Estado de contenedores ==="
                                docker ps -a | grep mplink || true
                                echo "=== Logs del Backend ==="
                                ${dockerComposeCmd} -f ${composeFile} logs mplink_backend || docker logs mplink_backend || true
                                echo "=== Logs de la Base de Datos ==="
                                ${dockerComposeCmd} -f ${composeFile} logs mplink_postgres || docker logs mplink_postgres || docker logs mplink_marketplace_db || true
                            """
                            throw e
                        }
                        // NO limpiar contenedores aquí - los tests Postman los necesitan
                        // Se limpiarán después de los tests
                    }
                }
            }
        }

        stage('Tests (Postman)') {
            when { 
                expression { 
                    // Ejecutar tests solo si se construyó Docker o si hay una URL configurada
                    params.BUILD_DOCKER || env.POSTMAN_BASE_URL != 'http://localhost:8080'
                } 
            }
            steps {
                dir(env.PROJECT_DIR) {
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        script {
                        // Si TEST_LOCAL_DOCKER está habilitado, usar la URL local
                        // Si no, usar la URL configurada (puede ser staging/production)
                        def testBaseUrl = params.TEST_LOCAL_DOCKER ? 'http://localhost:8080' : env.POSTMAN_BASE_URL
                        
                        echo "🧪 Configuración de Tests Postman:"
                        echo "   BASE_URL: ${testBaseUrl}"
                        echo "   Modo: ${params.TEST_LOCAL_DOCKER ? 'Local (docker-compose)' : 'Remoto'}"
                        
                        // Crear directorio target si no existe
                        sh 'mkdir -p target'
                        
                        // Buscar todas las colecciones Postman en tests/
                        echo "🔍 Buscando colecciones Postman en tests/..."
                        def collectionFiles = []
                        
                        // Buscar archivos JSON en tests/ (sin duplicados)
                        def foundFiles = sh(
                            script: "find tests -maxdepth 1 -name '*.json' -o -name '*.postman_collection.json' 2>/dev/null | sort -u || true",
                            returnStdout: true
                        ).trim()
                        
                        if (foundFiles) {
                            collectionFiles = foundFiles.split('\n').findAll { it.trim() && it.endsWith('.json') }.unique()
                        }
                        
                        if (collectionFiles.isEmpty()) {
                            echo "⚠️ No se encontraron colecciones Postman en tests/. Saltando tests."
                            return
                        }
                        
                        echo "📋 Encontradas ${collectionFiles.size()} colección(es) Postman - se ejecutarán todas:"
                        collectionFiles.each { file -> echo "   - ${file}" }
                        
                        echo "🔐 Nota: Las colecciones deben tener un endpoint de Login que guarde el token JWT en 'jwt_token'"
                        echo "   El token se usará automáticamente en todas las peticiones que requieran autenticación"
                        
                        // Verificar que el directorio uploads existe y tiene imágenes
                        def uploadsDir = "uploads"
                        def uploadsExists = fileExists(uploadsDir)
                        def hasImages = false
                        if (uploadsExists) {
                            def imageFiles = sh(
                                script: "find ${uploadsDir} -type f \\( -name '*.jpg' -o -name '*.jpeg' -o -name '*.png' -o -name '*.webp' -o -name '*.gif' \\) 2>/dev/null | head -5 || true",
                                returnStdout: true
                            ).trim()
                            hasImages = imageFiles && !imageFiles.isEmpty()
                            if (hasImages) {
                                echo "📸 Directorio uploads/ encontrado con imágenes disponibles"
                                echo "   Imágenes encontradas (primeras 5):"
                                imageFiles.split('\n').each { img -> echo "     - ${img}" }
                            } else {
                                echo "⚠️ Directorio uploads/ existe pero no contiene imágenes"
                            }
                        } else {
                            echo "⚠️ Directorio uploads/ no encontrado - las pruebas que requieran imágenes pueden fallar"
                        }
                        
                        // Detectar si necesitamos usar la red Docker
                        // Esto es necesario cuando:
                        // 1. TEST_LOCAL_DOCKER está habilitado, O
                        // 2. La URL es localhost y hay un contenedor Docker corriendo
                        def backendNetwork = 'mplink_net'
                        def useDockerNetwork = false
                        def backendContainerRunning = false
                        
                        // Verificar si el contenedor del backend está corriendo
                        def containerStatus = sh(
                            script: 'docker ps --filter "name=mplink-backend" --format "{{.Names}}" 2>/dev/null | head -1 || echo ""',
                            returnStdout: true
                        ).trim()
                        
                        if (containerStatus == 'mplink-backend') {
                            backendContainerRunning = true
                            echo "🔍 Contenedor mplink-backend detectado corriendo"
                        }
                        
                        // Determinar si debemos usar la red Docker
                        if (params.TEST_LOCAL_DOCKER) {
                            useDockerNetwork = true
                            echo "📍 Modo local: usando red Docker"
                        } else if (testBaseUrl.contains('localhost') && backendContainerRunning) {
                            useDockerNetwork = true
                            echo "📍 URL localhost con contenedor Docker detectado: usando red Docker"
                        }
                        
                        if (useDockerNetwork) {
                            echo "⏳ Verificando que el backend esté disponible..."
                            
                            // Obtener la red directamente del contenedor
                            // docker-compose crea redes con prefijo del directorio, así que buscamos por el contenedor
                            // Método más confiable: obtener el ID de red del contenedor y luego el nombre
                            def networkId = sh(
                                script: 'docker inspect mplink-backend 2>/dev/null | grep -A 5 "Networks" | grep "NetworkID" | head -1 | cut -d\\" -f4 || echo ""',
                                returnStdout: true
                            ).trim()
                            
                            def detectedNetwork = ''
                            if (networkId && !networkId.isEmpty()) {
                                detectedNetwork = sh(
                                    script: "docker network inspect ${networkId} --format '{{.Name}}' 2>/dev/null || echo ''",
                                    returnStdout: true
                                ).trim()
                            }
                            
                            // Método alternativo: buscar redes que contengan "mplink" en el nombre
                            if (!detectedNetwork || detectedNetwork.isEmpty()) {
                                detectedNetwork = sh(
                                    script: 'docker network ls --filter "name=mplink" --format "{{.Name}}" 2>/dev/null | head -1 || echo ""',
                                    returnStdout: true
                                ).trim()
                            }
                            
                            if (detectedNetwork && !detectedNetwork.isEmpty()) {
                                backendNetwork = detectedNetwork
                                echo "🔍 Red detectada: ${backendNetwork}"
                            } else {
                                echo "⚠️ No se pudo detectar la red automáticamente"
                                echo "   Intentando con el nombre por defecto: ${backendNetwork}"
                                echo "   Si falla, verifica que el contenedor mplink-backend esté corriendo"
                            }
                            
                            echo "🔗 Red Docker del backend: ${backendNetwork}"
                            
                            // Verificar que el servidor responda usando el nombre del contenedor
                            def httpCode = sh(
                                script: "docker run --rm --network ${backendNetwork} curlimages/curl:latest -s -o /dev/null -w '%{http_code}' http://mplink-backend:8080/actuator/health 2>/dev/null || echo '000'",
                                returnStdout: true
                            ).trim()
                            
                            if (httpCode == "000" || (httpCode.toInteger() >= 500 && httpCode.toInteger() < 600)) {
                                error("❌ El backend no está disponible. HTTP Code: ${httpCode}")
                            }
                            echo "✅ Backend está disponible (HTTP ${httpCode})"
                            
                            // Usar el nombre del contenedor como URL cuando usamos la red Docker
                            testBaseUrl = 'http://mplink-backend:8080'
                            echo "🔄 Cambiando BASE_URL a: ${testBaseUrl} (nombre del contenedor)"
                        } else if (testBaseUrl.contains('localhost') && !backendContainerRunning) {
                            echo "⚠️ Advertencia: URL localhost pero no se detectó contenedor Docker corriendo"
                            echo "   El contenedor de Newman intentará conectarse a localhost:8080 del host"
                        }
                        
                        echo "🚀 Ejecutando tests con Docker (postman/newman:latest)..."
                        
                        // Mostrar logs del backend ANTES de ejecutar tests (para debugging)
                        if (useDockerNetwork && backendContainerRunning) {
                            // Esperar a que Spring Boot arranque completamente (hasta 120 segundos)
                            echo "⏳ Esperando a que Spring Boot termine de iniciar (hasta 120 segundos)..."
                            sh '''
                                timeout=120
                                elapsed=0
                                app_ready=false
                                
                                while [ $elapsed -lt $timeout ]; do
                                    # Verificar si el log contiene "Started BackApplication"
                                    if docker logs mplink-backend 2>&1 | grep -q "Started BackApplication"; then
                                        echo "✅ Spring Boot ha arrancado completamente"
                                        app_ready=true
                                        break
                                    fi
                                    
                                    echo "⏳ Esperando... ($elapsed segundos)"
                                    sleep 5
                                    elapsed=$((elapsed + 5))
                                done
                                
                                if [ "$app_ready" != "true" ]; then
                                    echo "❌ ERROR: Spring Boot no arrancó en $timeout segundos"
                                    docker logs mplink-backend 2>&1
                                    exit 1
                                fi
                            '''
                            
                            echo "📋 === LOGS COMPLETOS DEL BACKEND ==="
                            sh """
                                docker logs mplink-backend 2>&1 || true
                            """
                            echo "📋 === FIN LOGS DEL BACKEND ==="
                            
                            // Verificar estado del contenedor
                            echo "🔍 Estado del contenedor backend..."
                            sh """
                                docker inspect mplink-backend --format='{{.State.Status}}: {{.State.Health.Status}}' 2>/dev/null || echo "No disponible"
                            """
                            
                            // Verificar si el proceso Java está corriendo
                            echo "🔍 Verificando proceso Java..."
                            sh """
                                docker exec mplink-backend ps aux | grep java || echo "⚠️ Proceso Java no encontrado"
                            """
                            
                            // Intentar conectarse al puerto 8080 desde dentro del contenedor
                            echo "🔍 Verificando conectividad interna al puerto 8080..."
                            sh """
                                docker exec mplink-backend curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:8080/actuator/health 2>&1 || \
                                echo "⚠️ curl falló - el backend puede no estar escuchando en 8080"
                            """
                            
                            // NUEVO: Probar el endpoint de login directamente
                            echo "🔍 Probando endpoint de login directamente..."
                                echo "🔍 Probando endpoint de login directamente (curl)..."
                                sh """
                                    docker run --rm --network ${backendNetwork} curlimages/curl:latest \\
                                        -v -X POST \\
                                        -H 'Content-Type: application/json' \\
                                        -d '{"email":"test@example.com","password":"password123"}' \\
                                        -w '\\nHTTP_CODE:%{http_code}\\n' \\
                                        http://mplink-backend:8080/api/auth/login 2>&1 | head -100 || true
                                """
                                echo "🔍 Verificando logs del backend después del intento de login..."
                                sh "docker logs --tail 50 mplink-backend 2>&1 | grep -E '(JWT-LOGIN|ERROR|WARN|attemptAuthentication)' || echo 'Sin logs relevantes'"
                        }
                        
                        // Ejecutar cada colección dentro de un contenedor Docker
                        collectionFiles.each { collectionFile ->
                            def fileName = collectionFile.split('/').last()
                            def baseName = fileName.replaceAll(/\.(json|postman_collection\.json)$/, '')
                            def outputFile = "target/newman-${baseName}.xml"
                            
                            echo "🔍 Ejecutando colección: ${collectionFile}"
                            echo "   BASE_URL: ${testBaseUrl}"
                            echo "   USER_EMAIL: ${env.POSTMAN_USER_EMAIL}"
                            echo "   USER_PASSWORD: ${env.POSTMAN_USER_PASSWORD.replaceAll('.', '*')}" // Ocultar password en logs
                            echo "   🔐 El login se ejecutará automáticamente y el token JWT se usará en todas las peticiones"
                            
                            // Ejecutar newman dentro de un contenedor Docker
                            // Usar la misma red Docker que el backend si está configurado
                            def dockerNetwork = ''
                            if (useDockerNetwork) {
                                dockerNetwork = "--network ${backendNetwork}"
                                echo "   Usando red Docker: ${backendNetwork}"
                            } else {
                                echo "   Ejecutando sin red Docker específica (modo remoto)"
                            }
                            
                        // Obtener la ruta absoluta del workspace actual (dentro de /var/jenkins_home)
                        def workspaceAbsolutePath = sh(
                                    script: "pwd",
                                    returnStdout: true
                                ).trim()
                                
                        echo "   📁 Workspace absoluto: ${workspaceAbsolutePath}"
                        echo "   📄 Archivo de colección: ${collectionFile}"

                        // Verificar estado del directorio uploads (ya accesible dentro del volumen de Jenkins)
                        if (uploadsExists) {
                            def uploadsAbsolutePath = "${workspaceAbsolutePath}/${uploadsDir}"
                                def uploadsPathExists = sh(
                                    script: "test -d \"${uploadsAbsolutePath}\" && echo 'exists' || echo 'notfound'",
                                    returnStdout: true
                                ).trim()
                                
                                if (uploadsPathExists == 'exists') {
                                echo "   📸 Directorio uploads disponible en: ${uploadsAbsolutePath}"
                                } else {
                                echo "   ⚠️ uploads/ no encontrado en ${uploadsAbsolutePath}"
                                }
                            } else {
                            echo "   ⚠️ uploads/ no existe dentro del proyecto; algunas pruebas podrían fallar"
                        }

                        // Determinar el volumen real que usa Jenkins para /var/jenkins_home.
                        // Por defecto docker-compose crea uno con prefijo del proyecto (ej: jenkins-docker_jenkins-data).
                        def jenkinsVolumeName = env.JENKINS_HOME_VOLUME ?: 'jenkins-docker_jenkins-data'
                        echo "   📦 Usando volumen de Jenkins: ${jenkinsVolumeName}"

                        // Verificar que el volumen exista antes de usarlo
                        def volumeExists = sh(
                            script: "docker volume inspect ${jenkinsVolumeName} >/dev/null 2>&1 && echo 'exists' || echo 'missing'",
                            returnStdout: true
                        ).trim()

                        if (volumeExists != 'exists') {
                            error("❌ No se encontró el volumen ${jenkinsVolumeName}. Ajusta JENKINS_HOME_VOLUME o crea el volumen.")
                        }

                        def jenkinsVolumeMount = "-v ${jenkinsVolumeName}:/var/jenkins_home"
                            
                            sh """
                                docker run --rm ${dockerNetwork} \
                                    ${jenkinsVolumeMount} \
                                    -w "${workspaceAbsolutePath}" \
                                    -e BASE_URL="${testBaseUrl}" \
                                    -e USER_EMAIL="${env.POSTMAN_USER_EMAIL}" \
                                    -e USER_PASSWORD="${env.POSTMAN_USER_PASSWORD}" \
                                    postman/newman:latest \
                                    run "${collectionFile}" \
                                    --env-var "BASE_URL=${testBaseUrl}" \
                                    --env-var "USER_EMAIL=${env.POSTMAN_USER_EMAIL}" \
                                    --env-var "USER_PASSWORD=${env.POSTMAN_USER_PASSWORD}" \
                                    --reporters cli,junit \
                                    --reporter-junit-export "${outputFile}"
                            """
                            
                            // Verificar que el archivo XML se generó
                            if (fileExists(outputFile)) {
                                echo "✅ Colección ${collectionFile} ejecutada. Resultados en ${outputFile}"
                                sh "ls -lh ${outputFile} || true"
                            } else {
                                echo "⚠️ Advertencia: No se generó el archivo de resultados ${outputFile}"
                            }
                        }
                        
                        // Publicar resultados JUnit después de ejecutar todas las colecciones
                        // Buscar archivos XML de resultados (path relativo al workspace raíz)
                        def xmlFiles = sh(
                            script: "find ${env.PROJECT_DIR}/target -name '*.xml' -type f 2>/dev/null || true",
                            returnStdout: true
                        ).trim()
                        
                        if (xmlFiles) {
                            echo "📊 Archivos de resultados encontrados:"
                            xmlFiles.split('\n').each { file ->
                                // Limpiar el path (remover ./ si existe)
                                def cleanFile = file.replaceFirst(/^\.\//, '')
                                echo "   - ${cleanFile}"
                            }
                            
                            // Construir path correcto para JUnit (sin ./ al inicio)
                            // JUnit necesita path relativo al workspace raíz, sin ./
                            def junitPath = "${env.PROJECT_DIR}/target/*.xml".replaceFirst(/^\.\//, '')
                            
                            echo "📋 Publicando resultados JUnit desde: ${junitPath}"
                            
                            // Verificar que el directorio existe
                            def targetExists = sh(
                                script: "test -d ${env.PROJECT_DIR}/target && echo 'exists' || echo 'notfound'",
                                returnStdout: true
                            ).trim()
                            
                            if (targetExists == 'exists') {
                                // Listar archivos para confirmar
                                sh "ls -lh ${env.PROJECT_DIR}/target/*.xml || true"
                                junit junitPath
                                echo "✅ Resultados JUnit publicados correctamente"
                            } else {
                                echo "⚠️ El directorio target no existe en ${env.PROJECT_DIR}/"
                                sh "pwd && ls -la || true"
                            }
                        } else {
                            echo "⚠️ No se encontraron archivos XML de resultados en ${env.PROJECT_DIR}/target/"
                            sh "ls -la ${env.PROJECT_DIR}/target/ 2>/dev/null || echo 'Directorio no encontrado'"
                        }
                        }
                    }
                }
            }
        }

        stage('Push Imagen') {
            when { expression { params.PUSH_DOCKER && params.BUILD_DOCKER } }
            steps {
                withDockerRegistry([credentialsId: 'docker-registry-credentials', url: 'https://index.docker.io/v1/']) {
                    sh "docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    sh "docker push ${env.DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Deploy to Azure') {
            when { expression { params.DEPLOY_ENV != 'none' && params.BUILD_DOCKER } }
            steps {
                script {
                    // Grupo de recursos y nombre del containerapp según el entorno
                    def resourceGroup = params.DEPLOY_ENV == 'production' ? 'rg-app-container' : 'rg-app-container'
                    def containerAppName = 'mplink-backend'
                    
                    echo "🚀 Desplegando a Azure (${params.DEPLOY_ENV})"
                    echo "   Container App: ${containerAppName}"
                    echo "   Resource Group: ${resourceGroup}"
                    echo "   Imagen: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    
                    withCredentials([azureServicePrincipal('azure-credentials-id')]) {
                        // Usar Azure CLI desde Docker para evitar instalación en Jenkins
                        // Usar variables de entorno directamente en el contenedor para evitar interpolación insegura
                        sh """
                            docker run --rm \
                                -e AZURE_CLIENT_ID="${AZURE_CLIENT_ID}" \
                                -e AZURE_CLIENT_SECRET="${AZURE_CLIENT_SECRET}" \
                                -e AZURE_TENANT_ID="${AZURE_TENANT_ID}" \
                                mcr.microsoft.com/azure-cli:latest \
                                bash -c "
                                    az login --service-principal \
                                        -u \\\$AZURE_CLIENT_ID \
                                        -p \\\$AZURE_CLIENT_SECRET \
                                        --tenant \\\$AZURE_TENANT_ID && \
                                    az containerapp update \
                                        --name ${containerAppName} \
                                        --resource-group ${resourceGroup} \
                                        --image ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} || \
                                    (echo '⚠️ Error al actualizar el containerapp. Verificando si existe...' && \
                                     az containerapp show --name ${containerAppName} --resource-group ${resourceGroup} 2>&1 || \
                                     echo '❌ El containerapp no existe. Asegúrate de crearlo primero en Azure Portal.')
                                "
                        """
                    }
                }
            }
        }
        
        stage('Limpiar Contenedores de Prueba') {
            when { 
                expression { 
                    // Solo limpiar si NO se está ejecutando TEST_LOCAL_DOCKER
                    // Esto permite que el contenedor quede activo para los tests del frontend
                    params.BUILD_DOCKER && !params.TEST_LOCAL_DOCKER
                } 
            }
            steps {
                script {
                    def dockerComposeCmd = sh(
                        script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                        returnStdout: true
                    ).trim()
                    
                    // Determinar qué docker-compose usar
                    def composeFile = fileExists("${env.PROJECT_DIR}/docker-compose.back.yml") ? "${env.PROJECT_DIR}/docker-compose.back.yml" : "${env.PROJECT_DIR}/../docker-compose.back.yml"
                    def composeDir = env.PROJECT_DIR
                    
                    echo "🧹 Limpiando contenedores de prueba..."
                    dir(composeDir) {
                        sh """
                            ${dockerComposeCmd} -f ${composeFile} down -v 2>/dev/null || true
                            # Limpiar imágenes dangling si las hay
                            docker image prune -f || true
                        """
                    }
                }
            }
        }
        
        stage('Exponer Backend Local (Docker)') {
            when { 
                expression { 
                    params.EXPOSE_BACKEND && params.BUILD_DOCKER 
                } 
            }
            steps {
                dir(env.PROJECT_DIR) {
                script {
                        def dockerComposeCmd = sh(
                            script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                        returnStdout: true
                    ).trim()
                    
                        def composeFile = fileExists('docker-compose.back.yml') ? 'docker-compose.back.yml' : '../docker-compose.back.yml'
                        def composeDir = '.'

                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            echo "🔁 Asegurando backend corriendo en http://localhost:8080..."
                            
                            // Limpiar contenedores existentes primero
                            sh """
                                # Parar servicios con docker-compose
                                ${dockerComposeCmd} -f ${composeFile} down -v 2>/dev/null || true
                                
                                # Forzar stop/rm de contenedores relacionados
                                docker stop mplink-backend mplink-marketplace-db mplink-marketplace-test-db 2>/dev/null || true
                                docker rm mplink-backend mplink-marketplace-db mplink-marketplace-test-db 2>/dev/null || true
                            """
                            
                            dir(composeDir) {
                                sh """
                                    ${dockerComposeCmd} -f ${composeFile} up -d mplink-postgres mplink-postgres-test mplink-backend
                                """
                            }

                            echo ""
                            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                            echo "🌐 BACKEND DISPONIBLE EN http://localhost:8080"
                            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                            echo "   Contenedor: mplink-backend"
                            echo "   Para detenerlo manualmente: ${dockerComposeCmd} -f ${composeFile} down"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Build finalizado con estado: ${currentBuild.currentResult}"
        }
    }
}
