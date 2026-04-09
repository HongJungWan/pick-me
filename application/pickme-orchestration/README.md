# pickme-orchestration (Temporal Workflow Engine)

> Temporal SDK 격리 모듈 — 워크플로우, 액티비티, Worker 구성, 모니터링

## 역할

Temporal SDK 의존성을 이 모듈에만 격리하고, 4개 워크플로우의 오케스트레이션 로직을 관리한다. 도메인 모듈은 `pickme-orchestration-api`의 CommandPort 인터페이스만 구현하며, `io.temporal.*` 패키지를 직접 참조하지 않는다.

## 워크플로우

| 워크플로우 | Workflow ID | Task Queue | 실행 타임아웃 | 특징 |
|-----------|------------|-----------|------------|------|
| `OrderFulfillmentWorkflow` | `order-fulfillment-{orderId}` | `pickme-order-saga` | 30분 | 4단계 Saga + 보상, @SignalMethod cancelByAdmin |
| `OrderRefundWorkflow` | `order-refund-{orderId}` | `pickme-order-saga` | 1시간 | 환불요청 → PG환불 → 재고복원 → 완료 |
| `SettlementReconciliationWorkflow` | `settlement-reconciliation-{date}` | `pickme-settlement` | 2시간 | 일일 스냅샷 조회 → 파트너별 검증 → 불일치 보고 |
| `PartnerOnboardingWorkflow` | `partner-onboarding-{registrationNumber}` | `pickme-partner` | 7일 | 등록 → Workflow.await(7d) 인간 승인 → 승인/거절/만료 |

## 주문 이행 사가 — 보상 매트릭스

```
Step 실패 지점              보상 액션
─────────────────          ──────────────────
1. reserveInventory 실패   → cancelOrder
2. processPayment 실패     → restoreInventory + cancelOrder
3. confirmOrder 실패       → refund + restoreInventory + cancelOrder
4. confirmInventory 실패   → 로그 경고 (주문/결제 확정 유지, 수동 조치)
```

## Activity 차등 RetryOptions

| Activity 용도 | 타임아웃 | 재시도 | 초기 간격 |
|--------------|---------|--------|----------|
| 내부 서비스 (재고/주문 확정) | 10초 | 5회 | 500ms |
| 외부 PG (결제) | 60초 | 3회 | 2초 |
| 정산 Reconciliation | 5분 | 3회 | 5초 |
| 파트너 온보딩 | 30초 | 3회 | 1초 |

## Worker 스케일링

| Task Queue | 동시 Activity | 동시 Workflow Task | 용도 |
|-----------|-------------|------------------|------|
| `pickme-order-saga` | 200 | 200 | 고빈도 주문 처리 |
| `pickme-settlement` | 50 | 50 | 일일 배치 |
| `pickme-partner` | 50 | 50 | 저빈도, 장기 대기 |

## REST API

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/v1/workflows/order-fulfillment/{orderId}` | GET | 워크플로우 상태 조회 |
| `/api/v1/workflows/order-fulfillment/{orderId}/cancel` | POST | 관리자 취소 시그널 |

## 모니터링

- `TemporalFailureMonitor` — 5분 주기 FAILED/TIMED_OUT 워크플로우 감지 → Slack 알림
- `TemporalMetricsConfig` — Temporal SDK 메트릭 → Micrometer/Prometheus 브릿지

## Feature Flag

```yaml
pickme:
  temporal:
    enabled: ${TEMPORAL_ENABLED:false}    # true: Temporal 오케스트레이션, false: Kafka 코레오그래피
    shadow-mode: ${TEMPORAL_SHADOW_MODE:false}  # true: dry-run 검증 (Kafka가 실제 상태 관리)
    target: ${TEMPORAL_ADDRESS:localhost:7233}
```

## 테스트

```bash
./gradlew :application:pickme-orchestration:test
```

14 시나리오: 정상 경로, 재고 부족, 결제 실패, 주문확정 실패, 상태 쿼리, 환불 3건, 정산 3건, 파트너 승인/거절/타임아웃
