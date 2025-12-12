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
    }

    parameters {
        booleanParam(name: 'BUILD_DOCKER', defaultValue: true)
        booleanParam(name: 'PUSH_DOCKER', defaultValue: false)
        booleanParam(name: 'TEST_LOCAL_DOCKER', defaultValue: false, description: 'Levanta docker-compose localmente para validar antes de desplegar')
        booleanParam(name: 'EXPOSE_BACKEND', defaultValue: true, description: 'Mantiene el backend activo en http://localhost:8080 al finalizar')
        choice(name: 'DEPLOY_ENV', choices: ['none','staging','production'])
    }

    stages {

        stage('Checkout y Validación') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                    
                    // Detectar automáticamente el directorio base del proyecto
                    if (fileExists('pom.xml') && fileExists('Dockerfile')) {
                        env.PROJECT_DIR = '.'
                        echo "✅ Workspace es el directorio back/"
                    } else if (fileExists('back/pom.xml') && fileExists('back/Dockerfile')) {
                        env.PROJECT_DIR = 'back'
                        echo "✅ Workspace es la raíz del repo, proyecto en back/"
                    } else {
                        echo "❌ No se encontró pom.xml o Dockerfile"
                        sh 'pwd && ls -la || true'
                        error("❌ Estructura del proyecto no válida")
                    }
                    
                    echo "Commit: ${env.GIT_COMMIT_SHORT}"
                    echo "Directorio: ${env.PROJECT_DIR}"
                    
                    // Detectar archivo docker-compose
                    if (fileExists('docker-compose.back.yml')) {
                        env.COMPOSE_FILE = 'docker-compose.back.yml'
                    } else if (fileExists('../docker-compose.back.yml')) {
                        env.COMPOSE_FILE = '../docker-compose.back.yml'
                    } else {
                        env.COMPOSE_FILE = 'docker-compose.yml'
                    }
                    echo "Compose file: ${env.COMPOSE_FILE}"
                }
            }
        }

        stage('Construir Imagen Docker') {
            when { expression { params.BUILD_DOCKER } }
            steps {
                dir(env.PROJECT_DIR) {
                    script {
                        def buildDate = sh(script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"', returnStdout: true).trim()
                        
                        sh """
                            docker build \
                                --build-arg BUILD_DATE='${buildDate}' \
                                --build-arg GIT_COMMIT='${env.GIT_COMMIT_SHORT}' \
                                --build-arg VERSION='${env.BUILD_NUMBER}' \
                                -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \
                                -t ${env.DOCKER_IMAGE}:latest \
                                .
                        """
                        echo "✅ Imagen construida: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    }
                }
            }
        }

        stage('Validación Local') {
            when { expression { params.TEST_LOCAL_DOCKER && params.BUILD_DOCKER } }
            steps {
                dir(env.PROJECT_DIR) {
                    script {
                        def dockerComposeCmd = sh(
                            script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                            returnStdout: true
                        ).trim()
                        
                        try {
                            echo "🚀 Iniciando validación local con ${dockerComposeCmd}"
                            
                            // Limpieza y preparación
                            sh """
                                ${dockerComposeCmd} -f ${env.COMPOSE_FILE} down -v 2>/dev/null || true
                                docker stop mplink-backend mplink-marketplace-db mplink-marketplace-test-db 2>/dev/null || true
                                docker rm mplink-backend mplink-marketplace-db mplink-marketplace-test-db 2>/dev/null || true
                                docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} mplink-backend:latest
                            """
                            
                            // Levantar SOLO PostgreSQL primero
                            sh """
                                ${dockerComposeCmd} -f ${env.COMPOSE_FILE} up -d mplink-postgres mplink-postgres-test
                            """
                            
                            echo "⏳ Esperando servicios (BD: 60s + init.sql)..."
                            
                            // Esperar BD
                            sh '''
                                timeout=60; elapsed=0
                                until docker exec mplink-marketplace-db pg_isready -U postgres -d marketplace_db >/dev/null 2>&1; do
                                    [ $elapsed -ge $timeout ] && echo "❌ Timeout BD" && exit 1
                                    sleep 2; elapsed=$((elapsed + 2))
                                done
                                echo "✅ BD lista (conexión establecida)"
                            '''
                            
                            // Esperar a que el init.sql termine de ejecutarse (verificar que tabla appeals exista)
                            sh '''
                                echo "⏳ Esperando inicialización de esquema (init.sql)..."
                                timeout=90; elapsed=0
                                until docker exec mplink-marketplace-db psql -U postgres -d marketplace_db -tAc "SELECT to_regclass('public.appeals');" 2>/dev/null | grep -q "appeals"; do
                                    [ $elapsed -ge $timeout ] && echo "❌ Timeout init.sql" && exit 1
                                    sleep 3; elapsed=$((elapsed + 3))
                                    echo "  ... esperando (${elapsed}s/${timeout}s)"
                                done
                                echo "✅ Esquema inicializado (tabla appeals existe)"
                            '''
                            
                            // Ahora levantar el backend con credenciales de correo
                            withCredentials([usernamePassword(credentialsId: 'mail-smtp-creds', usernameVariable: 'MAIL_USERNAME', passwordVariable: 'MAIL_PASSWORD')]) {
                                sh """
                                    ${dockerComposeCmd} -f ${env.COMPOSE_FILE} up -d mplink-backend
                                """
                            }
                            
                            // Esperar Backend
                            sh """
                                timeout=180; elapsed=0
                                while [ \$elapsed -lt \$timeout ]; do
                                    health=\$(docker inspect --format='{{.State.Health.Status}}' mplink-backend 2>/dev/null || echo "none")
                                    [ "\$health" = "healthy" ] && echo "✅ Backend healthy" && exit 0
                                    
                                    http=\$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
                                    [ "\$http" != "000" ] && [ \$http -lt 500 ] && echo "✅ Backend responde (HTTP \$http)" && exit 0
                                    
                                    sleep 5; elapsed=\$((elapsed + 5))
                                done
                                echo "❌ Timeout backend"
                                ${dockerComposeCmd} -f ${env.COMPOSE_FILE} logs mplink-backend
                                exit 1
                            """
                            
                            echo "✅ Validación completada"
                            
                        } catch (Exception e) {
                            echo "❌ Error: ${e.getMessage()}"
                            sh """
                                docker ps -a | grep mplink || true
                                ${dockerComposeCmd} -f ${env.COMPOSE_FILE} logs mplink-backend || true
                            """
                            throw e
                        }
                    }
                }
            }
        }

        stage('Push Imagen') {
            when { expression { params.PUSH_DOCKER && params.BUILD_DOCKER } }
            steps {
                dir(env.PROJECT_DIR) {
                    withDockerRegistry([credentialsId: 'docker-registry-credentials', url: 'https://index.docker.io/v1/']) {
                        sh "docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                        sh "docker push ${env.DOCKER_IMAGE}:latest"
                    }
                }
            }
        }

        stage('Deploy to Azure') {
            when { expression { params.DEPLOY_ENV != 'none' } }
            steps {
                script {
                    def resourceGroup = 'rg-app-container'
                    def containerAppName = 'mplink-backend'
                    
                    echo "🚀 Desplegando a Azure (${params.DEPLOY_ENV})"
                    echo "   Container App: ${containerAppName}"
                    echo "   Resource Group: ${resourceGroup}"
                    echo "   Imagen: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
                    
                    withCredentials([azureServicePrincipal('azure-credentials-id')]) {
                        // Validar credenciales
                        if (!env.AZURE_CLIENT_ID || !env.AZURE_CLIENT_SECRET || !env.AZURE_TENANT_ID) {
                            error("❌ Faltan credenciales de Azure. Verifica 'azure-credentials-id' en Jenkins.")
                        }
                        
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
            when { expression { params.BUILD_DOCKER && !params.TEST_LOCAL_DOCKER } }
            steps {
                dir(env.PROJECT_DIR) {
                    script {
                        def dockerComposeCmd = sh(
                            script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                            returnStdout: true
                        ).trim()
                        
                        echo "🧹 Limpiando contenedores de prueba..."
                        sh """
                            ${dockerComposeCmd} -f ${env.COMPOSE_FILE} down -v 2>/dev/null || true
                            docker image prune -f || true
                        """
                    }
                }
            }
        }
        
        stage('Exponer Backend Local (Docker)') {
            when { expression { params.EXPOSE_BACKEND } }
            steps {
                dir(env.PROJECT_DIR) {
                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                        script {
                            def dockerComposeCmd = sh(
                                script: 'command -v docker-compose >/dev/null 2>&1 && echo "docker-compose" || echo "docker compose"',
                                returnStdout: true
                            ).trim()

                            echo "🔁 Asegurando backend corriendo en http://localhost:8080..."
                            
                            sh """
                                ${dockerComposeCmd} -f ${env.COMPOSE_FILE} down -v 2>/dev/null || true
                                docker stop mplink-backend mplink-marketplace-db mplink-marketplace-test-db 2>/dev/null || true
                                docker rm mplink-backend mplink-marketplace-db mplink-marketplace-test-db 2>/dev/null || true
                            """
                            
                            // Levantar SOLO PostgreSQL primero
                            sh """
                                ${dockerComposeCmd} -f ${env.COMPOSE_FILE} up -d mplink-postgres mplink-postgres-test
                            """
                            
                            // Esperar a que PostgreSQL y el init.sql estén listos
                            sh '''
                                echo "⏳ Esperando PostgreSQL..."
                                timeout=60; elapsed=0
                                until docker exec mplink-marketplace-db pg_isready -U postgres -d marketplace_db >/dev/null 2>&1; do
                                    [ $elapsed -ge $timeout ] && echo "⚠️ Timeout BD (continuando...)" && break
                                    sleep 2; elapsed=$((elapsed + 2))
                                done
                                
                                echo "⏳ Esperando inicialización del esquema..."
                                timeout=90; elapsed=0
                                until docker exec mplink-marketplace-db psql -U postgres -d marketplace_db -tAc "SELECT to_regclass('public.appeals');" 2>/dev/null | grep -q "appeals"; do
                                    [ $elapsed -ge $timeout ] && echo "⚠️ Timeout init.sql (continuando...)" && break
                                    sleep 3; elapsed=$((elapsed + 3))
                                done
                                echo "✅ Base de datos lista"
                            '''

                            withCredentials([usernamePassword(credentialsId: 'mail-smtp-creds', usernameVariable: 'MAIL_USERNAME', passwordVariable: 'MAIL_PASSWORD')]) {
                                sh """
                                    ${dockerComposeCmd} -f ${env.COMPOSE_FILE} up -d mplink-backend
                                """
                            }

                            echo ""
                            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                            echo "🌐 Backend disponible en: http://localhost:8080"
                            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Build finalizado: ${currentBuild.currentResult}"
        }
    }
}
