# DDD 패턴은 왜 객체지향 같을까?

> 커머스 플랫폼(pick-me)을 DDD로 설계하면서 느낀 점을 정리했다.

---

## 이상한 기시감

DDD(Domain-Driven Design) 전술적 패턴을 처음 공부할 때 묘한 기시감이 들었다.

- "Entity는 자신의 상태를 스스로 변경해야 한다" → 이거 캡슐화 아닌가?
- "Value Object는 불변이어야 한다" → Java의 String이 이미 이렇잖아?
- "Repository는 인터페이스로 추상화하라" → 의존성 역전이잖아?

처음에는 "DDD가 OOP를 다른 이름으로 포장한 건가?" 싶었다. 하지만 실제로 코드를 작성해보니, **같은 문제를 다른 관점에서 바라보는 것**이었다. OOP가 "좋은 코드 구조"를 추구한다면, DDD는 "좋은 코드 구조가 비즈니스 문제를 정확히 반영하는가"를 추구한다.

이 글에서는 pick-me 프로젝트의 실제 코드를 통해, DDD 패턴을 적용하다 보면 왜 자연스럽게 좋은 객체지향 코드가 되는지 이야기해본다.

---

## 1. "주문 취소"를 어디에 작성할 것인가

프로젝트에서 가장 먼저 부딪힌 질문이다. 주문 취소 로직을 어디에 넣을까?

### 처음에 작성하기 쉬운 코드

```java
// OrderService에서 주문 취소
if (order.getStatus() == OrderStatus.PLACED) {
    order.setStatus(OrderStatus.CANCELLED);
}
```

동작은 한다. 하지만 이 코드에는 문제가 있다.

"PLACED 상태에서만 취소 가능하다"는 **비즈니스 규칙이 Service에 흩어져 있다.** 주문 취소를 호출하는 곳이 10군데라면, 10군데 모두 같은 if문을 작성해야 한다. 하나라도 빠뜨리면 PAID 상태인 주문이 취소된다.

### DDD가 제안하는 코드

```java
// Order 안에서 취소
public void cancel(String reason) {
    changeStatus(OrderStatus.CANCELLED);
    domainEvents.add(new OrderCancelledEvent(orderId.getValue(), reason, linePayloads));
}

private void changeStatus(OrderStatus newStatus) {
    if (!this.status.canTransitionTo(newStatus)) {
        throw new IllegalStateException("주문 상태 전이 불가: " + this.status + " → " + newStatus);
    }
    this.status = newStatus;
}
```

이제 Service는 `order.cancel("고객 요청")` 한 줄만 호출한다. **"어떤 상태에서 취소 가능한가"는 Order가 안다.** Service가 알 필요가 없다.

이것이 DDD에서 말하는 **Rich Domain Model**이고, OOP에서 말하는 **캡슐화**다. 같은 코드를 놓고 DDD는 "도메인 규칙이 도메인 안에 있다"고 설명하고, OOP는 "데이터와 행위가 하나의 객체에 묶여 있다"고 설명한다. **설명하는 언어가 다를 뿐, 코드는 같다.**

---

## 2. 금액을 long으로 쓸 것인가, Money로 쓸 것인가

주문 금액을 `long totalAmount`로 선언하면 아무 문제 없어 보인다. 하지만 이런 코드가 생기기 시작한다.

```java
long total = 0;
for (OrderLine line : orderLines) {
    total += line.getUnitPrice() * line.getQuantity();  // 음수가 들어오면?
}
```

금액이 음수가 되면? 통화 단위가 다르면? 이런 검증이 모든 계산 로직에 반복된다.

### DDD가 제안하는 코드 — Value Object

```java
public class Money {
    private final long amount;

    public Money(long amount) {
        if (amount < 0) throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
        this.amount = amount;
    }

    public Money add(Money other) { return new Money(this.amount + other.amount); }

    @Override
    public boolean equals(Object o) { ... Objects.equals(amount, ((Money) o).amount); }
}
```

`Money`를 만들면 **"금액은 0 이상"이라는 규칙이 Money 안에 한 번만 존재한다.** 어디서 Money를 사용하든 음수 금액은 만들어지지 않는다.

같은 패턴이 다른 곳에서도 반복된다.

```java
// Email — 형식 검증이 생성자에 있다
public class Email {
    private final String value;
    public Email(String value) {
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다: " + value);
        }
        this.value = value.toLowerCase().strip();
    }
}

// Quantity — 수량 부족 체크가 연산 안에 있다
public class Quantity {
    private final int value;
    public Quantity subtract(int amount) {
        if (this.value < amount) throw new IllegalStateException("수량 부족");
        return new Quantity(this.value - amount);
    }
}
```

DDD에서는 이것을 **Value Object**라 부른다. OOP에서는 **불변 객체**라 부른다. Java의 `String`, `Integer`, `BigDecimal`이 이미 이 패턴이다. DDD가 새로운 개념을 만든 게 아니라, **"도메인의 값을 타입으로 명시하라"는 기준을 준 것**이다.

Entity(주문)는 식별자가 있어서 "ORD-001과 ORD-002는 다른 주문"이지만, Money(금액)는 식별자가 없어서 "29,900원은 어디서든 29,900원"이다.

---

## 3. Order의 내부를 밖에서 건드리면 안 되는 이유

Order에는 `List<OrderLine> orderLines`가 있다. 만약 외부에서 이 리스트를 직접 수정할 수 있다면?

```java
order.getOrderLines().add(new OrderLine(...));  // totalAmount와 불일치 발생
```

총 금액은 그대로인데 주문 항목이 하나 추가된다. **데이터 정합성이 깨진다.**

### 해결: Aggregate

```
Order (Aggregate Root) ─── 외부는 이것만 접근 가능
  ├── OrderId (VO)
  ├── OrderLine (VO) ─── 외부에서 직접 수정 불가
  ├── ShippingInfo (VO)
  │     └── Address (VO)
  ├── Money (VO)
  └── OrderStatus (Enum)
```

```java
public class Order implements DomainEventProvider {
    private final List<OrderLine> orderLines;

    public List<OrderLine> getOrderLines() {
        return Collections.unmodifiableList(orderLines);  // 불변 리스트 반환
    }
}
```

DDD에서는 이것을 **Aggregate(집합체)** 라 부른다. Order가 Aggregate Root이고, 외부에서는 반드시 Order를 통해서만 내부 상태에 접근한다.

OOP에서는 이것을 **정보 은닉(Information Hiding)** 이라 부른다. private 필드를 외부에 직접 노출하지 않는 원칙이다. DDD의 Aggregate는 이 원칙을 단일 객체가 아니라 **관련 객체 군집 단위로 확장한 것**이다.

---

## 4. 도메인은 데이터베이스를 모른다

Order를 DB에 저장해야 한다. 가장 쉬운 방법은 Order에 JPA 어노테이션을 붙이는 것이다.

```java
@Entity  // ← 도메인 객체가 JPA에 의존
@Table(name = "orders")
public class Order { ... }
```

하지만 이러면 Order가 JPA라는 **인프라스트럭처에 의존**한다. DB를 바꾸면 도메인 코드도 바꿔야 한다.

### 해결: Repository (Port/Adapter)

```
pickme-order/
  ├── domain/
  │     └── repository/
  │           └── OrderRepository.java        ← 인터페이스 (Port)
  └── infrastructure/
        └── persistence/
              ├── OrderJpaEntity.java          ← JPA 전용 엔티티
              ├── OrderMapper.java             ← Domain ↔ JPA 변환
              └── OrderRepositoryImpl.java     ← 구현체 (Adapter)
```

```java
// 도메인 — JPA, Spring 어노테이션이 없다
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId orderId);
}

// 인프라 — JPA 구현체
@Repository
public class OrderRepositoryImpl implements OrderRepository {
    @Override
    public Order save(Order order) {
        return OrderMapper.toDomain(jpaRepository.save(OrderMapper.toJpaEntity(order)));
    }
}
```

도메인의 `Order.java`에는 `@Entity`, `@Column`이 **없다**. JPA 전용 엔티티(`OrderJpaEntity`)는 별도로 존재하고, `OrderMapper`가 둘 사이를 변환한다.

이 프로젝트에서는 ArchUnit으로 이 규칙을 CI에서 강제한다.

```java
@ArchTest
static final ArchRule 도메인은_스프링에_의존하지_않는다 =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("org.springframework..");
```

DDD에서는 이것을 **도메인 순수성**이라 부르고, OOP/SOLID에서는 **의존성 역전 원칙(DIP)** 이라 부른다. 의존 방향이 항상 바깥(infrastructure) → 안쪽(domain)을 향한다.

---

## 5. new Order()가 아니라 Order.place()인 이유

```java
Order order = new Order(id, ordererId, lines, status, shipping, total, now);
```

이 코드만 보면 이 Order가 "새로 접수된 주문"인지, "DB에서 복원된 주문"인지 알 수 없다.

```java
// 주문 접수 — 이벤트가 발행된다
public static Order place(UUID ordererId, List<OrderLine> orderLines, ShippingInfo shippingInfo) {
    if (orderLines.isEmpty()) {
        throw new IllegalArgumentException("주문 항목은 1개 이상이어야 합니다");
    }
    Money total = orderLines.stream()
            .map(OrderLine::getLineTotal).reduce(Money.zero(), Money::add);

    Order order = new Order(OrderId.generate(), ordererId, orderLines,
            OrderStatus.PLACED, shippingInfo, total, Instant.now());
    order.domainEvents.add(new OrderPlacedEvent(...));
    return order;
}

// DB 복원 — 이벤트가 발행되지 않는다
public static Order reconstitute(OrderId orderId, UUID ordererId, ...) {
    return new Order(orderId, ordererId, orderLines, status, shippingInfo, totalAmount, orderedAt);
}
```

`place`라는 이름이 **"고객이 주문을 접수하는 행위"** 라는 비즈니스 맥락을 전달한다. 프로젝트 전체에서 이 패턴이 일관된다.

```java
Product.register(...)    // 상품을 "등록"한다
Payment.request(...)     // 결제를 "요청"한다
Member.register(...)     // 회원을 "가입"시킨다
Stock.create(...)        // 재고를 "생성"한다
```

GoF에서는 이것을 **팩토리 메서드 패턴**이라 부른다. DDD는 여기에 **유비쿼터스 언어(Ubiquitous Language)** — 도메인 전문가와 개발자가 공유하는 비즈니스 용어 — 라는 맥락을 더한다. OOP에서 "생성 로직의 캡슐화"를 추구한다면, DDD에서는 "생성 행위에 비즈니스 의미를 부여"한다.

---

## 6. Entity에 넣기 어색한 로직은 어디에 두는가

결제 처리를 생각해보자. PG사를 호출하고, 결과에 따라 Payment 상태를 바꿔야 한다.

이 로직을 Payment Entity에 넣으면?

```java
// Payment 안에 PG 호출 로직? → 도메인이 인프라에 의존
public void process(PgPaymentGateway gateway) { ... }
```

Application Service에 넣으면?

```java
// Service에서 PG 호출 후 상태 변경? → 비즈니스 로직이 Service에 누수
PgResponse response = gateway.requestPayment(...);
if (response.isSuccess()) {
    payment.complete(response);
} else {
    payment.fail(response.getMessage());
}
```

### 해결: Domain Service

```java
// 도메인 패키지에 위치, Spring 어노테이션 없음
public class PaymentProcessingService {

    public interface PgGateway {  // 도메인이 정의한 Port
        PgResponse requestPayment(UUID paymentId, long amount, String method);
    }

    private final PgGateway pgGateway;

    public Payment processNewPayment(UUID orderId, UUID payerId, long amount, PaymentMethod method) {
        Payment payment = Payment.request(orderId, payerId, amount, method);
        payment.process();
        PgResponse response = pgGateway.requestPayment(...);

        if (response.isSuccess()) {
            payment.complete(response);
        } else {
            payment.fail(response.getMessage());
        }
        return payment;
    }
}
```

Application Service는 이 Domain Service를 호출만 한다.

```java
// Application Service — 오케스트레이션만
@Transactional
public void handleOrderPlaced(...) {
    Payment payment = paymentProcessingService.processNewPayment(...);
    paymentRepository.save(payment);
    eventPublisher.publishAll(payment);
}
```

Domain Service는 **DB 저장도 안 하고, 트랜잭션도 안 걸고, 이벤트 발행도 안 한다.** 순수하게 "PG 호출 + 결과에 따른 상태 전이"라는 비즈니스 규칙만 담당한다.

OOP에서는 이것을 **단일 책임 원칙(SRP)** 으로 설명한다. "이 로직이 이 클래스에 있어야 하나?"라는 질문에 대한 답이 Domain Service다.

---

## 7. 주문이 접수되면 재고가 차감되어야 한다

Order와 Inventory는 다른 Bounded Context다. Order가 Inventory를 직접 호출하면 강한 결합이 생긴다.

```java
// ❌ 강한 결합
orderService.createOrder(...);
inventoryService.reserve(...);  // Order가 Inventory를 알아야 한다
```

### 해결: Domain Event

```
[Order.place() 호출]
    ↓
Order 내부에서 OrderPlacedEvent 생성
    ↓
Outbox에 저장 (같은 트랜잭션)
    ↓
Kafka로 비동기 발행
    ↓
[Inventory] reserve() → InventoryReservedEvent
[Payment] processPayment() → PaymentCompletedEvent
[Notification] forOrderPlaced() → 알림 발송
```

핵심은 **이벤트를 Aggregate 내부에서 생성한다**는 것이다.

```java
public static Order place(UUID ordererId, List<OrderLine> orderLines, ShippingInfo shippingInfo) {
    Order order = new Order(...);
    order.domainEvents.add(new OrderPlacedEvent(...));  // Order가 이벤트를 만든다
    return order;
}
```

"주문이 접수되었다"는 사실은 Order가 가장 잘 안다. Service에서 `new OrderPlacedEvent(...)`를 만들면 **도메인 지식이 Service로 누수된다.**

Application Service는 이벤트를 Outbox에 저장하는 **발행 메커니즘**만 담당한다.

```java
orderRepository.save(order);
eventPublisher.publishAll(order);  // Outbox에 INSERT (같은 트랜잭션)
```

GoF에서는 이것을 **옵저버 패턴**이라 부른다. DDD에서는 **Domain Event**라 부른다. "누가 듣고 있는지 모르지만, 일어난 사실을 알려준다"는 원칙이 같다.

---

## 8. 7개 서비스에 같은 코드가 반복된다면

처음에 이벤트 발행 코드가 7개 서비스에 복제되어 있었다.

```java
// ProductService, OrderService, PaymentEventHandler, InventoryEventHandler,
// PartnerService, AuthService, SettlementEventHandler — 7곳에 동일 코드
private void publishDomainEvents(Order order) {
    for (DomainEvent event : order.getDomainEvents()) {
        String payload = objectMapper.writeValueAsString(event);
        outboxRepository.save(OutboxEvent.from(event, payload));
    }
    order.clearDomainEvents();
}
```

### 해결: 인터페이스 추출

```java
// 모든 Aggregate Root가 구현하는 인터페이스
public interface DomainEventProvider {
    List<DomainEvent> getDomainEvents();
    void clearDomainEvents();
}

// 이벤트 발행 단일 컴포넌트
@Component
public class DomainEventPublisher {
    public void publishAll(DomainEventProvider provider) {
        for (DomainEvent event : provider.getDomainEvents()) {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(OutboxEvent.from(event, payload));
        }
        provider.clearDomainEvents();
    }
}
```

`DomainEventPublisher`는 Order인지 Payment인지 Stock인지 **알 필요가 없다.** `getDomainEvents()`를 호출할 수 있는 객체면 된다. 이것이 **다형성(Polymorphism)** 이다.

```java
eventPublisher.publishAll(order);    // Order든
eventPublisher.publishAll(payment);  // Payment든
eventPublisher.publishAll(stock);    // Stock이든 — 같은 호출
```

---

## 9. 재고 차감을 누가 판단하는가

마지막으로, OOP에서 가장 중요한 원칙 하나.

```java
// ❌ Service가 상태를 꺼내서 판단 (Ask)
if (stock.getQuantity().getValue() >= requestedQty) {
    stock.setQuantity(stock.getQuantity() - requestedQty);
    stock.setReservedQuantity(stock.getReservedQuantity() + requestedQty);
}

// ✅ Stock에게 시킨다 (Tell)
stock.reserve(quantity, orderId);
```

`stock.reserve()`의 내부를 보면:

```java
public void reserve(int qty, UUID orderId) {
    if (!quantity.isGreaterThanOrEqual(qty)) {
        domainEvents.add(new InventoryShortageEvent(...));
        return;
    }
    this.quantity = this.quantity.subtract(qty);
    this.reservedQuantity = this.reservedQuantity.add(qty);
    domainEvents.add(new InventoryReservedEvent(...));

    if (quantity.isZero()) {
        domainEvents.add(new StockDepletedEvent(...));
    }
}
```

외부에서는 `stock.reserve(2, orderId)` 한 줄이다. **수량 검증, 재고 차감, 예약 증가, 부족 이벤트, 소진 이벤트** — 이 모든 것이 Stock 안에 있다.

OOP에서는 이것을 **Tell, Don't Ask** 원칙이라 부른다. 객체에게 상태를 물어보고 내가 판단하지 말고, 행위를 시켜라. DDD의 Rich Domain Model은 이 원칙의 자연스러운 결과다.

---

## 그래서, DDD는 왜 객체지향 같을까?

DDD가 OOP를 베낀 게 아니다. **DDD는 "비즈니스 문제를 코드로 정확히 표현하려면 어떻게 해야 하는가?"를 고민한 결과**이고, **OOP는 "변경에 유연하고 이해하기 쉬운 코드 구조는 무엇인가?"를 고민한 결과**다.

두 질문의 답이 겹치는 이유는, **비즈니스 규칙을 가장 잘 아는 객체가 그 규칙을 담당하는 것**이 결국 캡슐화이고, 정보 은닉이고, 단일 책임이기 때문이다.

| 코드에서 마주치는 질문 | DDD가 주는 답 | OOP가 주는 답 |
|----------------------|-------------|-------------|
| 이 비즈니스 규칙을 어디에 둘까? | Entity 안에 | 캡슐화 |
| 이 값의 유효성을 어디서 검증할까? | Value Object 생성자에서 | 불변 객체 |
| 외부에서 내부 상태를 직접 바꾸면? | Aggregate Root를 통해서만 | 정보 은닉 |
| 도메인이 DB를 알아야 하나? | Repository Interface로 분리 | 의존성 역전 |
| 객체 생성의 의도를 어떻게 표현하나? | Factory Method에 비즈니스 이름 | 팩토리 메서드 패턴 |
| Entity에 넣기 어색한 로직은? | Domain Service | 단일 책임 원칙 |
| 상태 변화를 다른 모듈에 어떻게 알리나? | Domain Event | 옵저버 패턴 |
| 상태를 꺼내서 판단하는 코드가 있다면? | 행위를 Aggregate에 위임 | Tell, Don't Ask |

DDD를 모르고 OOP를 잘 해도 캡슐화를 철저히 지키면 Rich Model이 나온다. OOP를 모르고 DDD 패턴만 따라해도 자연스럽게 캡슐화가 되고, SRP가 지켜지고, DIP가 적용된다.

**결국 좋은 코드는 같은 곳에 수렴한다.** DDD와 OOP는 서로 다른 출발점에서 같은 코드에 도달하는 두 개의 길이다.

---

> 이 글의 모든 코드 예시는 [pick-me](https://github.com/HongJungWan/pick-me) 커머스 플랫폼 프로젝트에서 가져왔습니다.
