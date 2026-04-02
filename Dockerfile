FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY common/pickme-common/build.gradle common/pickme-common/
COPY domain-modules/pickme-order/build.gradle domain-modules/pickme-order/
COPY domain-modules/pickme-payment/build.gradle domain-modules/pickme-payment/
COPY domain-modules/pickme-product/build.gradle domain-modules/pickme-product/
COPY domain-modules/pickme-inventory/build.gradle domain-modules/pickme-inventory/
COPY domain-modules/pickme-member/build.gradle domain-modules/pickme-member/
COPY domain-modules/pickme-partner/build.gradle domain-modules/pickme-partner/
COPY domain-modules/pickme-notification/build.gradle domain-modules/pickme-notification/
COPY domain-modules/pickme-settlement/build.gradle domain-modules/pickme-settlement/
COPY application/pickme-app/build.gradle application/pickme-app/
COPY application/pickme-gateway/build.gradle application/pickme-gateway/
COPY application/pickme-config-server/build.gradle application/pickme-config-server/
COPY application/pickme-discovery/build.gradle application/pickme-discovery/
COPY independent/pickme-archunit/build.gradle independent/pickme-archunit/
RUN gradle dependencies --no-daemon || true
COPY . .
RUN gradle :application:pickme-app:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pickme && adduser -S pickme -G pickme
COPY --from=builder /app/application/pickme-app/build/libs/*.jar app.jar
USER pickme
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
