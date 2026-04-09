# pickme-orchestration-api (Orchestration Contract)

> Temporal 워크플로우와 도메인 모듈 간의 Anti-Corruption Layer — 순수 Java 인터페이스/DTO

## 역할

도메인 모듈이 Temporal SDK에 직접 의존하지 않도록 **CommandPort 인터페이스**와 **오케스트레이션 DTO**를 정의한다. 이 모듈은 외부 의존성이 없는 순수 Java 모듈이다.

```
pickme-orchestration-api  (순수 Java — 의존성 없음)
        ↑                          ↑
        │                          │
  각 도메인 모듈                pickme-orchestration (Temporal SDK 격리)
  (CommandPort 구현)           (Workflow, Activity, Worker)
```

## CommandPort 인터페이스

도메인 모듈이 구현하고, Temporal Activity가 호출하는 포트 인터페이스:

| Port | 메서드 | 구현 모듈 |
|------|--------|----------|
| `OrderCommandPort` | `confirmOrder`, `cancelOrder`, `requestRefund`, `completeRefund` | pickme-order |
| `InventoryCommandPort` | `reserveInventory`, `confirmInventory`, `restoreInventory` | pickme-inventory |
| `PaymentCommandPort` | `processPayment`, `processRefund` | pickme-payment |
| `SettlementCommandPort` | `fetchDailySnapshots`, `reconcilePartner` | pickme-settlement |
| `PartnerCommandPort` | `registerPartner`, `approvePartner`, `rejectPartner` | pickme-partner |
| `WorkflowStarter` | `startOrderFulfillment`, `startRefund`, `startSettlementReconciliation`, `startPartnerOnboarding` | pickme-orchestration |

## DTO

| DTO | 용도 |
|-----|------|
| `OrderFulfillmentRequest` / `Result` | 주문 이행 사가 입출력 |
| `RefundRequest` / `Result` | 환불 워크플로우 입출력 |
| `PartnerOnboardingRequest` / `Result` | 파트너 온보딩 입출력 |
| `OrderLineItem` | 주문 라인 (productId, quantity, unitPrice) |
| `ReserveResult` / `PaymentResult` | Activity 실행 결과 |
| `WorkflowStepStatus` | 워크플로우 단계 enum (STARTED ~ COMPLETED) |

## 설계 원칙

- **Temporal SDK 의존 없음** — `build.gradle`에 외부 의존성이 없다
- **도메인 모델 참조 없음** — Record DTO는 오케스트레이션 계약이지 도메인 모델이 아니다
- **ArchUnit 강제** — `TemporalIsolationTest`가 도메인 레이어의 orchestration 참조를 차단
