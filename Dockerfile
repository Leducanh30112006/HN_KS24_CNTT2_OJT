# Minimal Production Runtime for QuickShop Spring Boot Microservices
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S quickshop && adduser -S quickshop -G quickshop

ARG SERVICE_NAME
COPY ${SERVICE_NAME}/build/libs/*.jar app.jar
RUN chown -R quickshop:quickshop /app

USER quickshop:quickshop

# Lightweight JVM tuning for VPS (SerialGC uses minimal CPU & RAM)
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms64m -Xmx160m -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom -Djava.net.preferIPv4Stack=true"

EXPOSE 8080 8081 8082 8083 8084 8085 8086

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
