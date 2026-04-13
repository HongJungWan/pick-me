package com.pickme.archunit;

import com.pickme.common.event.DomainEventProvider;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * DDD 전술 패턴 검증.
 *
 * <h3>검증 대상</h3>
 * <ul>
 *   <li><b>Aggregate Root</b>: {@link DomainEventProvider} 구현체. private 생성자 + 정적 팩토리 메서드</li>
 *   <li><b>ValueObject</b>: {@code ..domain.model..} 의 비-Aggregate 클래스. 모든 필드 final</li>
 *   <li><b>도메인 model 공통</b>: setter 메서드 금지</li>
 *   <li><b>DomainEvent</b>: {@code ..domain.event..} 패키지에만 위치</li>
 * </ul>
 *
 * <p>일부 규칙은 기존 코드에 위반이 존재할 수 있어 {@link FreezingArchRule}로
 * 베이스라인 처리한다 (Phase 2와 동일한 archunit_store 사용).</p>
 */
@ArchTestBase
class DddTacticalPatternTest {

    private static final String DOMAIN_EVENT_PROVIDER_FQN =
            "com.pickme.common.event.DomainEventProvider";

    /** 클래스가 DomainEventProvider를 구현(직접/상속)하는지 검사. */
    private static final DescribedPredicate<JavaClass> IS_AGGREGATE_ROOT =
            new DescribedPredicate<JavaClass>("DomainEventProvider 구현체 (Aggregate Root)") {
                @Override
                public boolean test(JavaClass clazz) {
                    return clazz.getAllRawInterfaces().stream()
                            .anyMatch(i -> i.getName().equals(DOMAIN_EVENT_PROVIDER_FQN));
                }
            };

    /** Aggregate Root가 아닌 클래스 (ValueObject 후보). */
    private static final DescribedPredicate<JavaClass> IS_NOT_AGGREGATE_ROOT =
            new DescribedPredicate<JavaClass>("Aggregate Root 아님") {
                @Override
                public boolean test(JavaClass clazz) {
                    return !IS_AGGREGATE_ROOT.test(clazz);
                }
            };

    /** 클래스의 모든 명시적 생성자가 private인지 확인하는 조건. */
    private static final ArchCondition<JavaClass> HAVE_ONLY_PRIVATE_CONSTRUCTORS =
            new ArchCondition<JavaClass>("정확히 private 생성자만 가져야 한다") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    clazz.getConstructors().forEach(constructor -> {
                        if (!constructor.getModifiers().contains(JavaModifier.PRIVATE)) {
                            events.add(SimpleConditionEvent.violated(
                                    constructor,
                                    String.format("Aggregate Root '%s' 의 생성자가 private 이 아니다: %s",
                                            clazz.getName(), constructor.getDescription())
                            ));
                        }
                    });
                }
            };

    /** 클래스의 모든 인스턴스 필드가 final인지 확인하는 조건. */
    private static final ArchCondition<JavaClass> HAVE_ONLY_FINAL_INSTANCE_FIELDS =
            new ArchCondition<JavaClass>("모든 인스턴스 필드는 final 이어야 한다") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    clazz.getFields().forEach(field -> {
                        var modifiers = field.getModifiers();
                        // static 필드는 검사 대상에서 제외
                        if (modifiers.contains(JavaModifier.STATIC)) return;
                        if (!modifiers.contains(JavaModifier.FINAL)) {
                            events.add(SimpleConditionEvent.violated(
                                    field,
                                    String.format("ValueObject 의 인스턴스 필드가 final 이 아니다: %s",
                                            field.getFullName())
                            ));
                        }
                    });
                }
            };

    // ─────────────────────────────────────────────────────────────────────
    // 1. 도메인 model 패키지의 클래스는 public setter 메서드를 갖지 않는다
    //    (Aggregate Root와 ValueObject 공통 — DDD: Tell, Don't Ask)
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule 도메인_model_클래스는_setter_메서드를_가지지_않는다 =
            FreezingArchRule.freeze(
                    noMethods()
                            .that().areDeclaredInClassesThat()
                            .resideInAnyPackage(DomainModules.subPackageMatchers("domain.model"))
                            .should().haveNameStartingWith("set")
                            .as("도메인 model 패키지의 클래스는 setter 메서드를 가지지 않는다")
                            .because("DDD: 도메인 객체 상태는 비즈니스 의미를 가진 메서드로만 변경한다 (Tell, Don't Ask).")
            );

    // ─────────────────────────────────────────────────────────────────────
    // 2. Aggregate Root(= DomainEventProvider 구현체)는 public 생성자를 갖지 않는다
    //    (정적 팩토리 메서드 또는 reconstitute 패턴 사용)
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule Aggregate_Root는_public_생성자를_가지지_않는다 =
            FreezingArchRule.freeze(
                    classes()
                            .that(IS_AGGREGATE_ROOT)
                            .and().resideInAnyPackage(DomainModules.subPackageMatchers("domain.model"))
                            .should(HAVE_ONLY_PRIVATE_CONSTRUCTORS)
                            .as("Aggregate Root 는 private 생성자만 가지고 정적 팩토리 메서드로 생성된다")
                            .because("DDD: Aggregate 생성 시점의 invariant 검증을 정적 팩토리에 강제한다.")
            );

    // ─────────────────────────────────────────────────────────────────────
    // 3. ValueObject(= Aggregate Root 아닌 도메인 model 클래스)는 모든 필드가 final
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule ValueObject는_모든_인스턴스_필드가_final이다 =
            FreezingArchRule.freeze(
                    classes()
                            .that().resideInAnyPackage(DomainModules.subPackageMatchers("domain.model"))
                            .and().areNotInterfaces()
                            .and().areNotEnums()
                            .and(IS_NOT_AGGREGATE_ROOT)
                            .should(HAVE_ONLY_FINAL_INSTANCE_FIELDS)
                            .as("ValueObject 는 불변 (모든 인스턴스 필드가 final)")
                            .because("DDD: ValueObject 는 동일성 비교를 위해 불변이어야 한다.")
            );

    // ─────────────────────────────────────────────────────────────────────
    // 4. DomainEvent 구현체는 ..domain.event.. 패키지에만 위치한다
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule DomainEvent_구현체는_domain_event_패키지에만_위치한다 =
            FreezingArchRule.freeze(
                    classes()
                            .that().implement("com.pickme.common.event.DomainEvent")
                            .and().resideInAnyPackage(DomainModules.packageMatchers())
                            .should().resideInAnyPackage(DomainModules.subPackageMatchers("domain.event"))
                            .as("도메인 이벤트 클래스는 domain.event 패키지에만 위치한다")
                            .because("이벤트는 도메인 모델의 일부이며 한 곳에서 카탈로그화한다.")
            );
}
