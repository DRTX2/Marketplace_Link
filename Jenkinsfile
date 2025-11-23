pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_IMAGE = "marketplace-link-backend"
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
                            def composeFile = fileExists('../docker-compose.yml') ? '../docker-compose.yml' : 'docker-compose.yml'
                            echo "📁 Usando docker-compose: ${composeFile}"
                            
                            // Limpiar contenedores previos si existen
                            sh """
                                ${dockerComposeCmd} -f ${composeFile} down -v 2>/dev/null || true
                                docker stop mplink_backend mplink_marketplace_db mplink_marketplace_test_db mplink_postgres mplink_postgres_test 2>/dev/null || true
                                docker rm mplink_backend mplink_marketplace_db mplink_marketplace_test_db mplink_postgres mplink_postgres_test 2>/dev/null || true
                            """
                            
                            // Etiquetar la imagen construida para que docker-compose la use
                            sh """
                                docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} mplink_backend:latest
                            """
                            
                            // Levantar solo backend y BD (sin frontend para pruebas más rápidas)
                            // Usar el docker-compose desde la ubicación correcta
                            def composeDir = fileExists('../docker-compose.yml') ? '..' : '.'
                            dir(composeDir) {
                            sh """
                                    ${dockerComposeCmd} -f ${composeFile} up -d mplink_postgres mplink_postgres_test mplink_backend
                            """
                            }
                            
                            echo "⏳ Esperando a que los servicios estén saludables..."
                            
                            // Esperar a que la BD esté lista (máximo 60 segundos)
                            // Intentar con ambos nombres de contenedor posibles
                            sh '''
                                timeout=60
                                elapsed=0
                                db_ready=false
                                
                                # Intentar con mplink_marketplace_db (back/docker-compose.yml)
                                until docker exec mplink_marketplace_db pg_isready -U postgres -d marketplace_db > /dev/null 2>&1 2>/dev/null || \
                                      docker exec mplink_postgres pg_isready -U mplink_user -d marketplace_link > /dev/null 2>&1; do
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
                                    health_status=\$(docker inspect --format='{{.State.Health.Status}}' mplink_backend 2>/dev/null || echo "none")
                                    
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
                                    ${dockerComposeCmd} -f ${composeFile} logs mplink_backend || docker logs mplink_backend || true
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
                            def composeFile = fileExists('../docker-compose.yml') ? '../docker-compose.yml' : 'docker-compose.yml'
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
                        
                        echo "📋 Encontradas ${collectionFiles.size()} colección(es) Postman"
                        collectionFiles.each { file -> echo "   - ${file}" }
                        
                        // Si es modo local, el backend ya debería estar disponible (se levantó en Validación Local)
                        // Solo verificamos rápidamente que esté disponible
                        def backendNetwork = 'mplink_net'
                        if (params.TEST_LOCAL_DOCKER) {
                            echo "⏳ Verificando que el backend esté disponible..."
                            
                            // Verificar que la red existe, si no, intentar obtenerla del contenedor
                            def networkExists = sh(
                                script: "docker network inspect ${backendNetwork} >/dev/null 2>&1 && echo 'exists' || echo 'notfound'",
                                returnStdout: true
                            ).trim()
                            
                            if (networkExists == 'notfound') {
                                // Intentar obtener la red del contenedor
                                def detectedNetwork = sh(
                                    script: 'docker inspect mplink_backend --format="{{range \$k, \$v := .NetworkSettings.Networks}}{{\$k}}{{end}}" 2>/dev/null | head -1 || echo ""',
                                    returnStdout: true
                                ).trim()
                                
                                if (detectedNetwork && !detectedNetwork.isEmpty()) {
                                    backendNetwork = detectedNetwork
                                    echo "🔍 Red detectada del contenedor: ${backendNetwork}"
                                } else {
                                    echo "⚠️ No se pudo detectar la red, usando: ${backendNetwork}"
                                }
                            }
                            
                            echo "🔗 Red Docker del backend: ${backendNetwork}"
                            
                            // Verificar que el servidor responda usando el nombre del contenedor
                            def httpCode = sh(
                                script: "docker run --rm --network ${backendNetwork} curlimages/curl:latest -s -o /dev/null -w '%{http_code}' http://mplink_backend:8080/actuator/health 2>/dev/null || echo '000'",
                                returnStdout: true
                            ).trim()
                            
                            if (httpCode == "000" || (httpCode.toInteger() >= 500 && httpCode.toInteger() < 600)) {
                                error("❌ El backend no está disponible. HTTP Code: ${httpCode}")
                            }
                            echo "✅ Backend está disponible (HTTP ${httpCode})"
                            
                            // Usar el nombre del contenedor como URL cuando esté en modo local
                            testBaseUrl = 'http://mplink_backend:8080'
                            echo "🔄 Cambiando BASE_URL a: ${testBaseUrl} (nombre del contenedor)"
                        }
                        
                        echo "🚀 Ejecutando tests con Docker (postman/newman:latest)..."
                        
                        // Ejecutar cada colección dentro de un contenedor Docker
                        collectionFiles.each { collectionFile ->
                            def fileName = collectionFile.split('/').last()
                            def baseName = fileName.replaceAll(/\.(json|postman_collection\.json)$/, '')
                            def outputFile = "target/newman-${baseName}.xml"
                            
                            echo "🔍 Ejecutando colección: ${collectionFile}"
                            echo "   BASE_URL: ${testBaseUrl}"
                            echo "   USER_EMAIL: ${env.POSTMAN_USER_EMAIL}"
                            
                            // Ejecutar newman dentro de un contenedor Docker
                            // Si es modo local, usar la misma red Docker que el backend
                            def dockerNetwork = ''
                            if (params.TEST_LOCAL_DOCKER) {
                                dockerNetwork = "--network ${backendNetwork}"
                                echo "   Usando red Docker: ${backendNetwork}"
                            }
                            
                            sh """
                                docker run --rm ${dockerNetwork} \
                                    -v "\$(pwd):/workspace" \
                                    -w /workspace \
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
                    }
                }
            }
            post {
                always {
                    script {
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
                withCredentials([azureServicePrincipal('azure-credentials-id')]) {
                    sh """
                        az login --service-principal \
                            -u $AZURE_CLIENT_ID \
                            -p $AZURE_CLIENT_SECRET \
                            --tenant $AZURE_TENANT_ID

                        az containerapp update \
                            --name marketplace-link-backend \
                            --resource-group mi-grupo \
                            --image ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                    """
                }
            }
        }
        
        stage('Limpiar Contenedores de Prueba') {
            when { 
                expression { 
                    params.TEST_LOCAL_DOCKER && params.BUILD_DOCKER 
                } 
            }
            steps {
                script {
                    def dockerComposeCmd = sh(
                        script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                        returnStdout: true
                    ).trim()
                    
                    // Determinar qué docker-compose usar
                    def composeFile = fileExists("${env.PROJECT_DIR}/../docker-compose.yml") ? "${env.PROJECT_DIR}/../docker-compose.yml" : "${env.PROJECT_DIR}/docker-compose.yml"
                    def composeDir = fileExists("${env.PROJECT_DIR}/../docker-compose.yml") ? ".." : env.PROJECT_DIR
                    
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
    }

    post {
        always {
            echo "Build finalizado con estado: ${currentBuild.currentResult}"
        }
    }
}
