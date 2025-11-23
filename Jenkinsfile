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
                            
                            // Limpiar contenedores previos si existen
                            sh """
                                ${dockerComposeCmd} down -v 2>/dev/null || true
                                docker stop mplink_backend mplink_marketplace_db mplink_marketplace_test_db 2>/dev/null || true
                                docker rm mplink_backend mplink_marketplace_db mplink_marketplace_test_db 2>/dev/null || true
                            """
                            
                            // Etiquetar la imagen construida para que docker-compose la use
                            sh """
                                docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} mplink_backend:latest
                            """
                            
                            // Levantar solo backend y BD (sin frontend para pruebas más rápidas)
                            sh """
                                ${dockerComposeCmd} up -d mplink_postgres mplink_postgres_test mplink_backend
                            """
                            
                            echo "⏳ Esperando a que los servicios estén saludables..."
                            
                            // Esperar a que la BD esté lista (máximo 60 segundos)
                            sh '''
                                timeout=60
                                elapsed=0
                                until docker exec mplink_marketplace_db pg_isready -U postgres -d marketplace_db > /dev/null 2>&1; do
                                    if [ $elapsed -ge $timeout ]; then
                                        echo "❌ Timeout esperando la base de datos"
                                        exit 1
                                    fi
                                    echo "Esperando base de datos... ($elapsed/$timeout segundos)"
                                    sleep 2
                                    elapsed=$((elapsed + 2))
                                done
                                echo "✅ Base de datos lista"
                            '''
                            
                            // Esperar a que el backend esté saludable (máximo 120 segundos)
                            sh """
                                timeout=120
                                elapsed=0
                                until curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; do
                                    if [ \$elapsed -ge \$timeout ]; then
                                        echo "❌ Timeout esperando el backend"
                                        ${dockerComposeCmd} logs backend
                                        exit 1
                                    fi
                                    echo "Esperando backend... (\$elapsed/\$timeout segundos)"
                                    sleep 3
                                    elapsed=\$((elapsed + 3))
                                done
                                echo "✅ Backend saludable"
                            """
                            
                            // Validar que el health check responde correctamente
                            echo "🔍 Verificando health check del backend..."
                            def healthResponse = sh(
                                script: 'curl -s http://localhost:8080/actuator/health',
                                returnStdout: true
                            ).trim()
                            
                            echo "Health check response: ${healthResponse}"
                            
                            if (!healthResponse.contains('"status":"UP"')) {
                                error("❌ Health check no está UP. Response: ${healthResponse}")
                            }
                            
                            // Test adicional: verificar que la API responde
                            echo "🔍 Verificando que la API está respondiendo..."
                            sh 'curl -f http://localhost:8080/actuator/health || exit 1'
                            
                            echo "✅ Validación local completada exitosamente"
                            
                        } catch (Exception e) {
                            echo "❌ Error durante la validación local: ${e.getMessage()}"
                            // Mostrar logs en caso de error
                            sh """
                                echo "=== Logs del Backend ==="
                                ${dockerComposeCmd} logs backend || true
                                echo "=== Logs de la Base de Datos ==="
                                ${dockerComposeCmd} logs mplink_postgres || true
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
                        if (params.TEST_LOCAL_DOCKER) {
                            echo "⏳ Verificando que el backend esté disponible..."
                            def healthCheck = sh(
                                script: "curl -f -s http://localhost:8080/actuator/health 2>/dev/null || echo 'not-ready'",
                                returnStdout: true
                            ).trim()
                            
                            if (!healthCheck.contains('"status":"UP"') && !healthCheck.contains('UP')) {
                                error("❌ El backend no está disponible. Health check: ${healthCheck}")
                            }
                            echo "✅ Backend está disponible"
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
                            // Si es modo local, usar network del host para acceder a localhost
                            def dockerNetwork = params.TEST_LOCAL_DOCKER ? '--network host' : ''
                            
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
                dir(env.PROJECT_DIR) {
                    script {
                        def dockerComposeCmd = sh(
                            script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                            returnStdout: true
                        ).trim()
                        
                        echo "🧹 Limpiando contenedores de prueba..."
                        sh """
                            ${dockerComposeCmd} down -v 2>/dev/null || true
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
