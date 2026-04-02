FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY pickme-common/build.gradle pickme-common/
COPY pickme-order/build.gradle pickme-order/
COPY pickme-payment/build.gradle pickme-payment/
COPY pickme-product/build.gradle pickme-product/
COPY pickme-inventory/build.gradle pickme-inventory/
COPY pickme-member/build.gradle pickme-member/
COPY pickme-partner/build.gradle pickme-partner/
COPY pickme-notification/build.gradle pickme-notification/
COPY pickme-settlement/build.gradle pickme-settlement/
COPY pickme-app/build.gradle pickme-app/
COPY pickme-archunit/build.gradle pickme-archunit/
RUN gradle dependencies --no-daemon || true
COPY . .
RUN gradle :pickme-app:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pickme && adduser -S pickme -G pickme
COPY --from=builder /app/pickme-app/build/libs/*.jar app.jar
USER pickme
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
