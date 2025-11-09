# ================================
# Etapa 1: Build - Compilar la aplicación
# ================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar solo lo necesario primero para aprovechar la caché de dependencias
COPY pom.xml .

# Descargar dependencias sin compilar código (para cachear)
RUN mvn dependency:go-offline -B

# Copiar el código fuente después para no invalidar la caché anterior
COPY src ./src

# Compilar el proyecto (omitimos tests)
RUN mvn clean package -DskipTests -q

# ================================
# Etapa 2: Runtime - Ejecutar la aplicación
# ================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Marketplace Link Team"
LABEL description="Marketplace Link Backend - Spring Boot Application"
LABEL version="0.0.1"

# Establecer zona horaria y utilidades mínimas
RUN apk add --no-cache \
      tzdata \
      curl \
      postgresql-client \
  && cp /usr/share/zoneinfo/America/New_York /etc/localtime \
  && echo "America/New_York" > /etc/timezone \
  && rm -rf /var/cache/apk/*

# Crear usuario sin privilegios
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copiar solo el artefacto compilado (JAR)
COPY --from=builder /app/target/*.jar app.jar

# Crear y asignar permisos a los directorios necesarios
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

# Variables de entorno
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=prod" \
    SERVER_PORT=8080 \
    TZ=America/New_York

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -fs http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Comando de ejecución
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
