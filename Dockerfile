# ==============================================================
# Multi-stage Dockerfile for QuickShop Spring Boot Microservices
# Optimized with Eclipse Temurin 17 Alpine & Gradle layer caching
# ==============================================================

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle executable & project configurations first
COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle .
COPY build.gradle .

# Copy common library and target service
COPY common-library ./common-library

ARG SERVICE_NAME
COPY ${SERVICE_NAME} ./${SERVICE_NAME}

# Grant execution rights and compile service jar
RUN chmod +x ./gradlew && \
    ./gradlew :${SERVICE_NAME}:bootJar -x test --no-daemon --parallel

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Create non-root system user for security hardening
RUN addgroup -S quickshop && adduser -S quickshop -G quickshop

ARG SERVICE_NAME
COPY --from=builder --chown=quickshop:quickshop /app/${SERVICE_NAME}/build/libs/*.jar app.jar

USER quickshop:quickshop

# Memory limits & G1GC tuning for VPS environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080 8081 8082 8083 8084 8085 8086

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
