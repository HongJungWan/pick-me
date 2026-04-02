# DDD 패턴은 왜 객체지향 같을까?

> 커머스 플랫폼(pick-me)을 DDD로 설계하면서 깨달은 것: **DDD의 전술적 패턴은 결국 OOP 원칙의 도메인 맥락 적용판이다.**

---

## 서론: "이거 OOP에서 이미 배운 거 아닌가?"

DDD(Domain-Driven Design)를 처음 공부하면 전술적 패턴이라는 것을 만난다. Entity, Value Object, Aggregate, Repository, Factory, Domain Service, Domain Event... 이 패턴들을 하나씩 읽다 보면 묘한 기시감이 든다.

- Entity의 "상태와 행위를 하나로 묶어라" → 이거 **캡슐화** 아닌가?
- Value Object의 "불변이어야 한다" → Java의 **String**이 이미 이렇게 동작하잖아?
- Repository의 "인터페이스로 추상화하라" → **의존성 역전 원칙(DIP)** 그 자체 아닌가?
- Factory의 "생성 로직을 캡슐화하라" → GoF의 **팩토리 메서드 패턴** 아닌가?

맞다. DDD의 전술적 패턴은 OOP 원칙을 **"비즈니스 도메인"이라는 맥락에 맞게 이름 붙이고 체계화한 것**이다.

OOP를 잘하면 자연스럽게 DDD스러운 코드가 나오고, DDD 패턴을 잘 따르면 자연스럽게 좋은 객체지향 코드가 된다. 둘은 서로를 보완하는 관계다.

이 글에서는 실제 커머스 프로젝트(pick-me)의 코드를 예시로, **10개의 DDD 전술적 패턴이 각각 어떤 OOP 원칙과 대응하는지** 하나씩 짚어본다.

---

## 1. Entity (Rich Model) = 캡슐화 + Tell, Don't Ask

### OOP에서 배운 것
캡슐화(Encapsulation)는 **데이터와 그 데이터를 조작하는 행위를 하나의 객체 안에 묶는 것**이다. 외부에서는 객체의 내부 상태를 직접 건드리지 않고, 객체가 제공하는 메서드를 통해서만 상호작용한다.

### DDD에서 부르는 이름
이것을 DDD에서는 **Rich Domain Model**이라 부른다. 단순히 getter/setter만 있는 데이터 주머니(Anemic Model)가 아니라, **자신의 비즈니스 규칙을 스스로 알고, 상태를 스스로 변경하는 객체**다.

### 실제 코드로 비교

```java
// ❌ Anemic Model (캡슐화 위반) — 외부에서 상태를 꺼내서 판단
if (order.getStatus() == OrderStatus.PLACED) {
    order.setStatus(OrderStatus.CANCELLED);  // setter로 직접 변경
}
```

이 코드의 문제는 **"어떤 상태에서 취소가 가능한가?"라는 비즈니스 규칙이 Order 바깥에 흩어져 있다**는 것이다. 이 규칙을 사용하는 곳이 10군데라면, 10군데 모두 동일한 if문을 작성해야 한다.

```java
// ✅ Rich Model (캡슐화 준수) — Order가 스스로 판단하고 변경
public void cancel(String reason) {
    // "지금 취소 가능한 상태인가?" Order 자신이 판단
    changeStatus(OrderStatus.CANCELLED);
    // 이벤트도 Order가 직접 발행
    domainEvents.add(new OrderCancelledEvent(orderId.getValue(), reason, linePayloads));
}

private void changeStatus(OrderStatus newStatus) {
    if (!this.status.canTransitionTo(newStatus)) {
        throw new IllegalStateException("주문 상태 전이 불가: " + this.status + " → " + newStatus);
    }
    this.status = newStatus;
}
```

이 코드에서 OrderService는 `order.cancel("고객 요청")` 한 줄만 호출한다. **"어떤 상태에서 취소 가능한가?"라는 비즈니스 규칙은 Order 안에 있다.** 이것이 캡슐화이고, DDD에서는 이것을 Rich Domain Model이라 부른다.

> **정리**: OOP의 캡슐화 = DDD의 Rich Entity. 같은 원칙, 다른 이름.

---

## 2. Value Object = 불변 객체 + 동등성(Equality)

### OOP에서 배운 것
Java의 `String`은 한번 생성하면 내용을 바꿀 수 없다(불변). `"hello".equals("hello")`는 `true`다(값으로 비교). 이것이 불변 객체(Immutable Object)와 동등성(Value Equality)이다.

### DDD에서 부르는 이름
이것을 DDD에서는 **Value Object(VO)** 라 부른다. 식별자(ID)가 없고, **값 자체가 곧 정체성**인 객체다.

### 실제 코드로 비교

```java
// Money VO — "29,900원"이라는 값 자체가 의미
public class Money {
    private final long amount;  // final → 불변

    public Money(long amount) {
        if (amount < 0) throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
        this.amount = amount;  // 생성 시점에 유효성 검증 (fail-fast)
    }

    // 새 객체를 반환 (기존 객체 수정 안 함 = 불변)
    public Money add(Money other) { return new Money(this.amount + other.amount); }

    // 같은 금액이면 같은 객체 (값으로 비교)
    @Override
    public boolean equals(Object o) { ... Objects.equals(amount, ((Money) o).amount); }
}

// Email VO — "test@example.com"이라는 값 자체가 의미
public class Email {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@...");
    private final String value;

    public Email(String value) {
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다: " + value);
        }
        this.value = value.toLowerCase().strip();  // 정규화도 생성자에서
    }
}

// Quantity VO — "50개"라는 수량 값
public class Quantity {
    private final int value;
    public Quantity(int value) {
        if (value < 0) throw new IllegalArgumentException("수량은 0 이상이어야 합니다");
        this.value = value;
    }
    public Quantity subtract(int amount) {
        if (this.value < amount) throw new IllegalStateException("수량 부족");
        return new Quantity(this.value - amount);  // 새 객체 반환 = 불변
    }
}
```

VO의 3가지 특징을 정리하면:

1. **불변**: `final` 필드, setter 없음, 연산 시 새 객체 반환
2. **생성자 검증**: 잘못된 값은 아예 객체가 만들어지지 않음 (fail-fast)
3. **값 동등성**: `equals()`/`hashCode()`로 값이 같으면 같은 객체

Entity와의 차이를 한 줄로 요약하면:

```
Order(주문) → Entity  (주문번호 ORD-001과 ORD-002는 다른 주문)
Money(금액) → VO      (29,900원은 어디서든 29,900원, 식별자 불필요)
```

> **정리**: OOP의 불변 객체(String, Integer...) = DDD의 Value Object. Java가 이미 이 패턴을 쓰고 있다.

---

## 3. Aggregate = 정보 은닉 + 일관성 경계

### OOP에서 배운 것
**정보 은닉(Information Hiding)** 은 객체의 내부 구현을 외부에서 직접 접근하지 못하게 막는 것이다. `private` 필드 + `public` 메서드 = 외부에는 필요한 인터페이스만 노출한다.

### DDD에서 부르는 이름
이것을 DDD에서는 **Aggregate**라 부른다. 관련된 Entity와 VO를 하나로 묶고, **Aggregate Root(루트 엔티티)를 통해서만 접근**하게 강제한다. 이것이 곧 **트랜잭션의 일관성 경계**가 된다.

### 실제 코드로 비교

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
    private final OrderId orderId;
    private final List<OrderLine> orderLines;  // 내부 엔티티

    // 외부에서 OrderLine을 직접 수정하지 못하도록 방어
    public List<OrderLine> getOrderLines() {
        return Collections.unmodifiableList(orderLines);  // 불변 리스트 반환
    }
}
```

만약 `order.getOrderLines().add(new OrderLine(...))`이 가능하다면, Order의 `totalAmount`와 실제 주문 항목이 불일치하는 상태가 만들어진다. **Aggregate Root가 이 일관성을 보장한다.**

OOP의 "private 필드를 노출하지 않는다"는 원칙이, DDD에서는 "Aggregate Root를 통해서만 내부 상태를 변경한다"로 확장된 것이다.

> **정리**: OOP의 정보 은닉 = DDD의 Aggregate 경계. 스케일만 다르다 (객체 → 객체 군집).

---

## 4. Repository = 의존성 역전 원칙 (DIP) + 인터페이스 분리

### OOP에서 배운 것
SOLID의 **의존성 역전 원칙(DIP)** 은 "고수준 모듈이 저수준 모듈에 의존하면 안 된다. 둘 다 추상화에 의존해야 한다"는 원칙이다. 비즈니스 로직(고수준)이 DB 구현체(저수준)에 직접 의존하면, DB를 바꿀 때 비즈니스 로직도 수정해야 한다.

### DDD에서 부르는 이름
이것을 DDD에서는 **Repository 패턴**이라 부른다. 도메인 패키지에 **인터페이스(Port)** 를 두고, 인프라 패키지에 **구현체(Adapter)** 를 둔다.

### 실제 코드와 구조

```
pickme-order/
  ├── domain/
  │     └── repository/
  │           └── OrderRepository.java        ← 인터페이스 (Port)
  └── infrastructure/
        └── persistence/
              ├── JpaOrderRepository.java     ← Spring Data JPA
              ├── OrderJpaEntity.java          ← JPA 전용 엔티티
              ├── OrderMapper.java             ← Domain ↔ JPA 변환
              └── OrderRepositoryImpl.java     ← Port 구현체 (Adapter)
```

```java
// 도메인 패키지 — JPA, Spring 어노테이션이 전혀 없다
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId orderId);
    List<Order> findByOrdererId(UUID ordererId);
}

// 인프라 패키지 — JPA 구현체
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    private final JpaOrderRepository jpaRepository;

    @Override
    public Order save(Order order) {
        // Domain Order → JPA Entity 변환 → DB 저장 → 다시 Domain Order로 변환
        return OrderMapper.toDomain(jpaRepository.save(OrderMapper.toJpaEntity(order)));
    }
}
```

도메인 모델(`Order.java`)에는 `@Entity`, `@Column` 같은 JPA 어노테이션이 **단 하나도 없다**. 나중에 PostgreSQL을 MongoDB로 바꿔도 도메인 코드는 수정할 필요가 없다. 바꿔야 할 것은 `infrastructure/persistence/` 안의 파일들뿐이다.

> **정리**: OOP의 DIP(인터페이스로 추상화) = DDD의 Repository(Port/Adapter). 의존 방향이 항상 "안쪽(도메인)"을 향한다.

---

## 5. Factory = 팩토리 메서드 패턴 + 유비쿼터스 언어

### OOP에서 배운 것
GoF 디자인 패턴 중 **팩토리 메서드 패턴**은 객체 생성 로직을 캡슐화하여, `new` 키워드를 직접 사용하는 대신 의미 있는 메서드를 통해 객체를 생성하는 것이다.

### DDD에서 부르는 이름
이것을 DDD에서는 **Factory**라 부른다. 여기에 한 가지가 더해지는데, 메서드 이름에 **유비쿼터스 언어(Ubiquitous Language)** — 도메인 전문가와 개발자가 공유하는 비즈니스 용어 — 를 사용한다.

### 실제 코드로 비교

```java
// Order — "주문을 접수한다"
public static Order place(UUID ordererId, List<OrderLine> orderLines, ShippingInfo shippingInfo) {
    if (orderLines == null || orderLines.isEmpty()) {
        throw new IllegalArgumentException("주문 항목은 1개 이상이어야 합니다");
    }
    Money total = orderLines.stream()
            .map(OrderLine::getLineTotal)
            .reduce(Money.zero(), Money::add);

    Order order = new Order(OrderId.generate(), ordererId, orderLines,
            OrderStatus.PLACED, shippingInfo, total, Instant.now());

    // 생성과 동시에 도메인 이벤트 발행
    order.domainEvents.add(new OrderPlacedEvent(...));
    return order;
}

// DB에서 복원할 때는 별도 팩토리 (이벤트 발행 안 함)
public static Order reconstitute(OrderId orderId, UUID ordererId, ...) {
    return new Order(orderId, ordererId, orderLines, status, shippingInfo, totalAmount, orderedAt);
}
```

각 모듈의 팩토리 이름을 보면 비즈니스 언어가 그대로 드러난다:

```java
Product.register(...)    // 상품을 "등록"한다
Order.place(...)         // 주문을 "접수"한다
Payment.request(...)     // 결제를 "요청"한다
Member.register(...)     // 회원을 "가입"시킨다
Stock.create(...)        // 재고를 "생성"한다
Partner.register(...)    // 파트너를 "등록"한다
```

`new Order(...)` 대신 `Order.place(...)`를 사용하는 이유는, 생성자만으로는 "이 객체가 어떤 비즈니스 맥락에서 만들어졌는지" 전달할 수 없기 때문이다. `place`라는 단어 하나가 "고객이 주문을 접수하는 행위"라는 맥락을 명확히 전달한다.

> **정리**: OOP의 팩토리 메서드 = DDD의 Factory. DDD는 여기에 비즈니스 언어라는 맥락을 더한다.

---

## 6. Domain Service = 단일 책임 원칙 (SRP)

### OOP에서 배운 것
**단일 책임 원칙(SRP)** 은 "클래스는 변경의 이유가 하나뿐이어야 한다"는 원칙이다. 하나의 클래스에 여러 관심사가 섞이면, 한쪽을 수정할 때 다른 쪽이 깨질 수 있다.

### DDD에서 부르는 이름
이것을 DDD에서는 **Domain Service**라 부른다. 특정 Entity에 넣기에 어색한 비즈니스 로직 — 예를 들어, 외부 PG사를 호출하고 그 결과에 따라 Payment 상태를 변경하는 로직 — 을 별도 클래스로 분리한다.

### 실제 코드로 비교

```java
// Domain Service — 도메인 패키지에 위치, Spring 어노테이션 없음!
public class PaymentProcessingService {

    // 도메인이 정의한 Port (인터페이스)
    public interface PgGateway {
        PgResponse requestPayment(UUID paymentId, long amount, String method);
        PgResponse requestRefund(String pgTransactionId, long amount);
    }

    private final PgGateway pgGateway;

    // PG 호출 + Payment 상태 전이 = 하나의 비즈니스 행위
    public Payment processNewPayment(UUID orderId, UUID payerId, long amount, PaymentMethod method) {
        Payment payment = Payment.request(orderId, payerId, amount, method);
        payment.process();

        PgResponse response = pgGateway.requestPayment(...);

        if (response.isSuccess()) {
            payment.complete(response);   // 성공/실패 판단은 도메인에서
        } else {
            payment.fail(response.getMessage());
        }
        return payment;
    }
}
```

Domain Service와 Application Service의 차이를 보면:

```java
// Application Service — 오케스트레이션만 (비즈니스 판단 없음)
@Transactional
public void handleOrderPlaced(UUID eventId, UUID orderId, UUID ordererId, long totalAmount) {
    Payment payment = paymentProcessingService.processNewPayment(...);  // 도메인 서비스에 위임
    paymentRepository.save(payment);          // 인프라: 저장
    eventPublisher.publishAll(payment);       // 인프라: 이벤트 발행
}

// Domain Service — 순수 비즈니스 로직만
public Payment processNewPayment(...) {
    // DB 저장 안 함, 트랜잭션 안 걸음, 이벤트 발행 안 함
    // 오직 "PG 호출하고 결과에 따라 Payment 상태를 바꾸는" 비즈니스 규칙만
}
```

Domain Service는 **Spring Bean 등록조차 infrastructure에서 한다** (도메인 순수성 유지):

```java
// infrastructure/config/PaymentDomainConfig.java
@Configuration
public class PaymentDomainConfig {
    @Bean
    public PaymentProcessingService paymentProcessingService(PgPaymentGateway pgPaymentGateway) {
        return new PaymentProcessingService(pgPaymentGateway);
    }
}
```

> **정리**: OOP의 SRP("이 로직이 이 클래스에 있어야 하나?") = DDD의 Domain Service("이 로직이 Entity에 있어야 하나?").

---

## 7. Domain Event = 옵저버 패턴 + 느슨한 결합

### OOP에서 배운 것
GoF의 **옵저버 패턴(Observer Pattern)** 은 "한 객체의 상태 변화를 관찰하는 다른 객체들에게 자동으로 알려주는 것"이다. 발행자는 구독자의 존재를 모르고, 구독자는 발행자의 내부 구현을 모른다. 이것이 **느슨한 결합(Loose Coupling)** 이다.

### DDD에서 부르는 이름
이것을 DDD에서는 **Domain Event**라 부른다. Aggregate의 상태가 변경되면 이벤트를 발행하고, 다른 Bounded Context가 이 이벤트를 구독하여 자신의 로직을 실행한다.

### 실제 이벤트 흐름

```
[Order.place() 호출]
    ↓
Order 내부에서 OrderPlacedEvent 생성
    ↓
Application Service가 Outbox에 저장 (같은 트랜잭션)
    ↓
OutboxRelayScheduler가 Kafka로 발행
    ↓
[Inventory] reserve() → InventoryReservedEvent
[Payment] processPayment() → PaymentCompletedEvent
[Notification] forOrderPlaced() → 알림 발송
```

핵심은 **이벤트를 Aggregate 내부에서 생성한다**는 것이다:

```java
// Order Aggregate 내부 — 이벤트는 비즈니스 메서드 안에서 생성
public static Order place(UUID ordererId, List<OrderLine> orderLines, ShippingInfo shippingInfo) {
    Order order = new Order(...);
    // 비즈니스 행위의 결과로 이벤트 발생
    order.domainEvents.add(new OrderPlacedEvent(
            order.orderId.getValue(), ordererId, linePayloads, total.getAmount()));
    return order;
}

// Application Service는 발행만 위임
orderRepository.save(order);
eventPublisher.publishAll(order);  // Outbox 테이블에 INSERT (같은 트랜잭션)
```

왜 Aggregate 안에서 생성하는가? **"주문이 접수되었다"는 사실은 Order가 가장 잘 알기 때문**이다. Service에서 `new OrderPlacedEvent(...)`를 만들면 도메인 지식이 Service로 누수된다.

> **정리**: OOP의 옵저버 패턴 = DDD의 Domain Event. "누가 듣고 있는지 모르지만 알려준다"는 원칙이 동일하다.

---

## 8. DomainEventProvider / Publisher = 전략 패턴 + DRY

### OOP에서 배운 것
**전략 패턴(Strategy Pattern)** 은 알고리즘을 인터페이스로 추상화하고, 구현체를 바꿔 끼울 수 있게 하는 것이다. **DRY(Don't Repeat Yourself)** 원칙은 같은 로직을 여러 곳에 복제하지 않는 것이다.

### DDD에서의 적용

처음에 7개 서비스에 동일한 이벤트 발행 코드가 복제되어 있었다. 이를 공통 인터페이스와 단일 컴포넌트로 추출했다:

```java
// 모든 Aggregate Root가 구현하는 인터페이스 (전략 패턴)
public interface DomainEventProvider {
    List<DomainEvent> getDomainEvents();
    void clearDomainEvents();
}

// 이벤트 발행을 담당하는 단일 컴포넌트 (DRY)
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

```java
// 7개 서비스가 동일한 패턴으로 사용
Order order = Order.place(...);
orderRepository.save(order);
eventPublisher.publishAll(order);  // ← Order든 Payment든 Stock이든 동일한 호출
```

`DomainEventProvider` 인터페이스 덕분에, `DomainEventPublisher`는 Order인지 Payment인지 Stock인지 **알 필요가 없다**. 그저 `getDomainEvents()`를 호출할 수 있는 객체면 된다. 이것이 **다형성(Polymorphism)** 이고, DDD에서는 이를 통해 이벤트 발행 로직의 중복을 제거했다.

> **정리**: OOP의 인터페이스 추상화 + DRY = DDD의 DomainEventProvider/Publisher 패턴.

---

## 9. 도메인 순수성 = 의존성 역전 원칙 (DIP)

### OOP에서 배운 것
SOLID의 **의존성 역전 원칙(DIP)** 은 "고수준 모듈(비즈니스 로직)이 저수준 모듈(프레임워크, DB)에 의존해서는 안 된다"는 원칙이다.

### DDD에서 부르는 이름
이것을 DDD에서는 **도메인 순수성(Domain Purity)** 이라 부른다. 도메인 패키지(`domain/`)에는 Spring, JPA 같은 프레임워크 코드가 **절대 들어가서는 안 된다**.

### 실제 적용: ArchUnit으로 CI에서 강제

```java
// ArchUnit 테스트 — 위반하면 빌드 자체가 실패한다
@ArchTest
static final ArchRule 도메인은_스프링에_의존하지_않는다 =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("org.springframework..");

@ArchTest
static final ArchRule 도메인은_JPA에_의존하지_않는다 =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..");
```

이 규칙이 지켜지면 패키지 구조가 자연스럽게 정리된다:

```
pickme-order/
  ├── domain/           ← 순수 Java만 (Spring, JPA 없음)
  │     ├── model/      ← Order, OrderLine, Money...
  │     ├── event/      ← OrderPlacedEvent...
  │     ├── repository/ ← OrderRepository (인터페이스만)
  │     └── service/    ← (도메인 서비스)
  ├── application/      ← @Service, @Transactional
  ├── api/              ← @RestController
  └── infrastructure/   ← @Entity(JPA), @Repository, Kafka, Redis
```

의존 방향은 항상 **바깥 → 안쪽**이다. domain은 아무것도 모르고, infrastructure가 domain을 알고 구현한다.

> **정리**: OOP의 DIP = DDD의 도메인 순수성. "도메인은 프레임워크를 모른다"는 원칙.

---

## 10. Tell, Don't Ask = 캡슐화의 실전 원칙

### OOP에서 배운 것
**Tell, Don't Ask** 원칙은 캡슐화를 실천하는 가장 구체적인 가이드라인이다. "객체에게 상태를 물어보고(Ask) 내가 판단하지 말고, 행위를 시켜라(Tell)"는 것이다.

### DDD에서의 적용
DDD의 Rich Domain Model이 바로 이 원칙의 구현체다.

```java
// ❌ Ask (나쁜 예) — Service가 상태를 꺼내서 판단
if (stock.getQuantity().getValue() >= requestedQty) {
    stock.setQuantity(stock.getQuantity() - requestedQty);
    stock.setReservedQuantity(stock.getReservedQuantity() + requestedQty);
}

// ✅ Tell (프로젝트 방식) — Stock에게 "예약해라"고 시킴
stock.reserve(quantity, orderId);
// Stock 내부에서 알아서 판단 + 상태 변경 + 이벤트 발행
```

Stock.reserve()의 내부를 보면, 이 원칙이 얼마나 강력한지 알 수 있다:

```java
public void reserve(int qty, UUID orderId) {
    if (!quantity.isGreaterThanOrEqual(qty)) {
        // 부족하면 부족 이벤트 발행 (Stock이 스스로 판단)
        domainEvents.add(new InventoryShortageEvent(...));
        return;
    }
    this.quantity = this.quantity.subtract(qty);
    this.reservedQuantity = this.reservedQuantity.add(qty);
    domainEvents.add(new InventoryReservedEvent(...));

    if (quantity.isZero()) {
        domainEvents.add(new StockDepletedEvent(...));  // 소진도 Stock이 판단
    }
}
```

외부에서는 `stock.reserve(2, orderId)` 한 줄만 호출한다. **수량 검증, 재고 차감, 예약 증가, 부족 이벤트, 소진 이벤트** — 이 모든 비즈니스 규칙이 Stock 안에 있다.

> **정리**: OOP의 Tell Don't Ask = DDD Rich Model의 핵심 동작 원리. 같은 원칙의 다른 표현이다.

---

## 결론: 결국 좋은 코드는 같은 곳에 수렴한다

| OOP 원칙 | DDD 전술적 패턴 | 핵심 질문 |
|----------|----------------|-----------|
| 캡슐화 | Entity (Rich Model) | "이 비즈니스 규칙이 객체 안에 있는가?" |
| 불변 객체 | Value Object | "이 값을 바꿀 이유가 있는가?" |
| 정보 은닉 | Aggregate | "외부에서 내부 상태를 직접 건드릴 수 있는가?" |
| DIP (의존성 역전) | Repository (Port) | "도메인이 인프라에 의존하는가?" |
| 팩토리 메서드 | Factory | "객체 생성의 의도가 코드에 드러나는가?" |
| SRP (단일 책임) | Domain Service | "이 로직이 이 클래스에 있어야 하는가?" |
| 옵저버 패턴 | Domain Event | "상태 변화를 누가 알아야 하는가?" |
| 다형성 + DRY | EventProvider/Publisher | "중복 코드를 인터페이스로 추상화했는가?" |
| DIP (의존성 역전) | 도메인 순수성 | "도메인이 프레임워크를 아는가?" |
| Tell, Don't Ask | Rich Model 행위 | "내가 판단하는가, 객체에게 시키는가?" |

DDD를 모르고 OOP를 잘 해도, 캡슐화를 철저히 지키면 Rich Model이 나온다. 불변 객체를 습관처럼 만들면 Value Object가 나온다. 인터페이스로 추상화하면 Repository 패턴이 나온다.

반대로, OOP를 잘 모르고 DDD 패턴만 충실히 따라해도, 자연스럽게 캡슐화가 되고 SRP가 지켜지고 DIP가 적용된다.

**결국 좋은 코드는 같은 곳에 수렴한다.** DDD와 OOP는 서로 다른 출발점에서 같은 결론에 도달한 것이다. 하나를 이해하면 다른 하나가 자연스럽게 따라온다.

---

> 이 글의 모든 코드 예시는 [pick-me](https://github.com/HongJungWan/pick-me) 커머스 플랫폼 프로젝트에서 가져왔습니다. MSA + DDD + EDA 아키텍처로 설계된 실제 운영 가능한 코드베이스입니다.
