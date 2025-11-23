pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        JAVA_HOME = "${tool 'JDK-21'}"
        MAVEN_HOME = "${tool 'Maven-3.9'}"
        PATH = "${MAVEN_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"

        DOCKER_IMAGE = "marketplace-link-backend"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
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
                }
                echo "Commit: ${env.GIT_COMMIT_SHORT}"
            }
        }

        stage('Validación de Proyecto') {
            steps {
                dir('back') {
                    sh 'test -f pom.xml'
                    sh 'test -f Dockerfile'
                    sh 'echo "Validación OK"'
                }
            }
        }

        stage('Compilar Backend') {
            steps {
                dir('back') {
                    sh 'mvn clean package -DskipTests=true'
                }
            }
        }

        stage('Tests (Postman)') {
            steps {
                dir('back') {
                    script {
                        // Si existe una sola colección
                        if (fileExists('tests/postman_collection.json')) {
                            sh """
                                newman run tests/postman_collection.json \
                                --reporters cli,junit \
                                --reporter-junit-export target/newman-results.xml
                            """
                        } 
                        // Si existen varias colecciones
                        else if (sh(script: "ls tests/*.json 2>/dev/null | wc -l", returnStdout: true).trim() != "0") {
                            sh """
                                for c in tests/*.json; do
                                  echo "Ejecutando colección: \$c"
                                  newman run "\$c" \
                                    --reporters cli,junit \
                                    --reporter-junit-export "target/newman-\$(basename \$c).xml"
                                done
                            """
                        } 
                        // Si no hay colecciones
                        else {
                            echo "⚠️ No se encontraron colecciones Postman. Saltando tests."
                        }
                    }
                }
            }
            post {
                always {
                    junit 'back/target/*.xml'
                }
            }
        }

        stage('Construir Imagen Docker') {
            when { expression { params.BUILD_DOCKER } }
            steps {
                dir('back') {
                    sh """
                        docker build \
                            -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \
                            -t ${env.DOCKER_IMAGE}:latest \
                            .
                    """
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

        stage('Validación Local (Docker Compose)') {
            when { 
                expression { 
                    params.TEST_LOCAL_DOCKER && params.BUILD_DOCKER 
                } 
            }
            steps {
                dir('back') {
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
                        } finally {
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
    }

    post {
        always {
            echo "Build finalizado con estado: ${currentBuild.currentResult}"
        }
    }
}
