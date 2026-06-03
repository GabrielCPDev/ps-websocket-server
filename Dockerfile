FROM gradle:8.4.0-jdk21 AS builder

WORKDIR /build
COPY --chown=gradle:gradle . .

RUN gradle bootJar -x test

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=0 \
    EUREKA_CLIENT_SERVICE_URL_DEFAULT_ZONE=http://ps-discovery:8761/eureka/ \
    EUREKA_CLIENT_REGISTER_WITH_EUREKA=true \
    EUREKA_CLIENT_FETCH_REGISTRY=true \
    EUREKA_INSTANCE_PREFER_IP_ADDRESS=false \
    KAFKA_BOOTSTRAP_SERVERS=kafka:9092


ENTRYPOINT ["java", "-jar", "app.jar"]