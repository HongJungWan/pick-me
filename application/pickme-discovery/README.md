# pickme-discovery (Service Discovery)

> Spring Cloud Netflix Eureka Server — MSA 서비스 등록/탐색

## 역할

MSA 전환 시 각 서비스가 자신을 등록하고, Gateway가 서비스 이름으로 라우팅할 수 있게 한다. 하드코딩 URL 대신 `lb://pickme-order` 형태로 서비스를 찾는다.

## Phase별 상태

| Phase | 상태 | 설명 |
|-------|------|------|
| Phase 1 (현재) | 대기 | 모듈러 모놀리스 — pickme-app이 단일 JAR로 실행 |
| Phase 2 | 선택적 | 일부 서비스 분리 시 등록/탐색 시작 |
| Phase 3 | 필수 | 모든 서비스가 Eureka에 등록, Gateway가 서비스명 기반 라우팅 |

## 포트

`8761` (Eureka 표준 포트)

## 대시보드

http://localhost:8761 — 등록된 서비스 목록, 인스턴스 상태 확인

## 서비스 등록 예시 (Phase 3)

```yaml
# 각 서비스의 application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```
