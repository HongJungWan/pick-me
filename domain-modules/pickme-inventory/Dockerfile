FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY common/pickme-common/ common/pickme-common/
COPY domain-modules/pickme-inventory/ domain-modules/pickme-inventory/
RUN gradle :domain-modules:pickme-inventory:bootJar --no-daemon -x test || true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pickme && adduser -S pickme -G pickme
COPY --from=builder /app/domain-modules/pickme-inventory/build/libs/*.jar app.jar
USER pickme
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
