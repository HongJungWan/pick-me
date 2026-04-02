FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY application/pickme-discovery/ application/pickme-discovery/
RUN gradle :application:pickme-discovery:bootJar --no-daemon -x test || true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pickme && adduser -S pickme -G pickme
COPY --from=builder /app/application/pickme-discovery/build/libs/*.jar app.jar
USER pickme
EXPOSE 8761
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
