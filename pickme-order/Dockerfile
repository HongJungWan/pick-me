FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY ../build.gradle ../settings.gradle ./
COPY ../gradle/ gradle/
COPY ../pickme-common/ pickme-common/
COPY ../pickme-order/ pickme-order/
RUN gradle :pickme-order:bootJar --no-daemon -x test || true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pickme && adduser -S pickme -G pickme
COPY --from=builder /app/pickme-order/build/libs/*.jar app.jar
USER pickme
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
