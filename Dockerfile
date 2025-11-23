FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q
FROM eclipse-temurin:21-jre-alpine

# Build arguments (usados por Jenkins)
ARG BUILD_DATE
ARG BUILD_TIME
ARG GIT_COMMIT
ARG VERSION

LABEL maintainer="Marketplace Link Team"
LABEL description="Marketplace Link Backend - Spring Boot Application"
LABEL version="${VERSION:-0.0.1}"
LABEL build-date="${BUILD_DATE}"
LABEL build-time="${BUILD_TIME}"
LABEL git-commit="${GIT_COMMIT}"

RUN apk add --no-cache \
      tzdata \
      curl \
      postgresql-client \
  && cp /usr/share/zoneinfo/America/New_York /etc/localtime \
  && echo "America/New_York" > /etc/timezone \
  && rm -rf /var/cache/apk/*
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring:spring

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    TZ=America/New_York
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -fs http://localhost:${SERVER_PORT}/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
