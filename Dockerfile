FROM gradle:8.4.0-jdk17 AS builder

WORKDIR /build
COPY --chown=gradle:gradle . .

RUN gradle bootJar -x test

FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENV SERVER_PORT=8083

ENTRYPOINT ["java", "-jar", "app.jar"]