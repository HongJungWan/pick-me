# pickme-config-server (Configuration Server)

> Spring Cloud Config Server — MSA 설정 중앙 집중 관리

## 역할

모든 서비스의 설정(Kafka, Redis, DB, Timeout, Circuit Breaker 등)을 **단일 서버에서 제공**한다. 서비스별 application-*.yml에 중복되던 설정을 Config Server로 통합하여, 설정 변경 시 **서비스 재배포 없이 반영** 가능.

## 중앙 설정 구조

```
config/
├── application.yml          ← 전체 서비스 공통 (Kafka, Redis, Timeout, Circuit Breaker)
├── pickme-order.yml         ← Order 서비스 전용 (포트, DB URL, 풀 크기)
├── pickme-payment.yml
├── pickme-product.yml
├── pickme-inventory.yml
├── pickme-member.yml        ← JWT secret 포함
├── pickme-partner.yml
├── pickme-notification.yml  ← Slack webhook URL 포함
├── pickme-settlement.yml
├── pickme-app.yml           ← Flyway, Zipkin 설정
└── pickme-gateway.yml       ← 서비스 라우팅 URL
```

## 설정 조회 API

```bash
# 전체 공통 설정
curl http://localhost:8888/application/default

# Order 서비스 설정 (공통 + 개별 병합)
curl http://localhost:8888/pickme-order/default

# Payment 서비스 설정
curl http://localhost:8888/pickme-payment/default
```

## 포트

`8888` (Spring Cloud Config 표준 포트)

## 주요 중앙 관리 설정

| 설정 항목 | 위치 | 설명 |
|----------|------|------|
| Kafka 공통 | `application.yml` | bootstrap-servers, consumer/producer 설정 |
| Redis 공통 | `application.yml` | host, port, 커넥션 풀 |
| Timeout | `application.yml` | 서비스별 요청 타임아웃 |
| Circuit Breaker | `application.yml` | Resilience4j 임계치, 슬라이딩 윈도우 |
| Outbox Relay | `application.yml` | `pickme.outbox.relay.enabled` (CDC 전환 후 false) |
| JWT Secret | `pickme-member.yml` | Access/Refresh 토큰 시크릿 |
| Slack Webhook | `pickme-notification.yml` | DLT 알림용 Webhook URL |

## 설정 백엔드

현재 `native` 프로필 사용 (classpath 기반 로컬 파일). Git 저장소 백엔드로 전환 가능.
