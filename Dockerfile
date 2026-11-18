# Minimal Production Runtime for QuickShop Spring Boot Microservices
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S quickshop && adduser -S quickshop -G quickshop

ARG SERVICE_NAME
COPY ${SERVICE_NAME}/build/libs/*.jar app.jar
RUN chown -R quickshop:quickshop /app

USER quickshop:quickshop

# Lightweight JVM tuning for 1GB VPS (SerialGC + C1 JIT + bounded threads/metaspace + disable SQL log spam)
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xms24m -Xmx112m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Dserver.tomcat.threads.max=20 -Dserver.tomcat.threads.min-spare=2 -Dspring.jpa.show-sql=false -Djava.security.egd=file:/dev/./urandom -Djava.net.preferIPv4Stack=true"

EXPOSE 8080 8081 8082 8083 8084 8085 8086

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
