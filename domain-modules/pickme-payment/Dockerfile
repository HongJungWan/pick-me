FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY ../build.gradle ../settings.gradle ./
COPY ../gradle/ gradle/
COPY ../pickme-common/ pickme-common/
COPY ../pickme-payment/ pickme-payment/
RUN gradle :pickme-payment:bootJar --no-daemon -x test || true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pickme && adduser -S pickme -G pickme
COPY --from=builder /app/pickme-payment/build/libs/*.jar app.jar
USER pickme
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
