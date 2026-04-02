# pickme-discovery (Service Discovery)

> Spring Cloud Netflix Eureka Server — MSA 서비스 등록/탐색

## 역할

MSA 전환 시 각 서비스가 자신을 등록하고, Gateway가 서비스 이름으로 라우팅할 수 있게 한다. 하드코딩 URL 대신 `lb://pickme-order` 형태로 서비스를 찾는다.

## 포트

`8761` (Eureka 표준 포트)

## 대시보드

http://localhost:8761 — 등록된 서비스 목록, 인스턴스 상태 확인
