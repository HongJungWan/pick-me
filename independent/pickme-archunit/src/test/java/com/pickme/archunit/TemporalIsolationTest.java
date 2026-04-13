package com.pickme.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Temporal SDK 격리 및 Activity/CommandAdapter 경계 규칙.
 *
 * <h3>Temporal 격리 원칙</h3>
 * <ul>
 *   <li>{@code io.temporal..}은 {@code application.pickme-orchestration} 모듈에서만 사용</li>
 *   <li>도메인 모듈은 orchestration 구현 세부사항에 의존하지 않음
 *       (대신 {@code common:pickme-orchestration-api}의 CommandPort 인터페이스를 구현)</li>
 *   <li>Temporal Activity 구현체는 도메인 모듈 패키지를 직접 호출하지 않고
 *       반드시 CommandPort를 경유</li>
 * </ul>
 */
@ArchTestBase
class TemporalIsolationTest {

    @ArchTest
    static final ArchRule temporal_SDK는_orchestration_모듈에서만_사용한다 =
            noClasses()
                    .that().resideOutsideOfPackage("..orchestration..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.temporal..")
                    .because("Temporal SDK 의존성은 orchestration 모듈에 격리되어야 한다.");

    @ArchTest
    static final ArchRule 도메인_모듈은_orchestration_구현_세부사항에_의존하지_않는다 =
            noClasses()
                    .that().resideInAnyPackage(DomainModules.subPackageMatchers("domain"))
                    .should().dependOnClassesThat()
                    .resideInAPackage("..orchestration..")
                    .because("도메인 계층은 orchestration 구현체(Activity, Workflow)를 알 수 없다. "
                            + "통신은 common:pickme-orchestration-api의 인터페이스로만 한다.");

    // ─────────────────────────────────────────────────────────────────────
    // Activity 격리 강화
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule ActivityInterface_구현체는_orchestration_activity_패키지에만_위치한다 =
            classes()
                    .that().areAnnotatedWith("io.temporal.activity.ActivityInterface")
                    .should().resideInAPackage("..orchestration.activity..")
                    .because("Temporal Activity 인터페이스는 orchestration.activity 하위에만 정의한다.");

    @ArchTest
    static final ArchRule Activity_구현체는_도메인_모듈을_직접_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..orchestration.activity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(DomainModules.packageMatchers())
                    .because("Activity 구현체는 도메인 모듈을 직접 호출하지 않고 "
                            + "common:pickme-orchestration-api의 CommandPort 인터페이스만 사용해야 한다.");

    // ─────────────────────────────────────────────────────────────────────
    // CommandAdapter ↔ CommandPort 매핑 강제
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule CommandAdapter는_orchestration_port의_인터페이스를_구현해야_한다 =
            classes()
                    .that().haveSimpleNameEndingWith("CommandAdapter")
                    .and().resideInAnyPackage(DomainModules.subPackageMatchers("application"))
                    .should().implement(
                            DescribedPredicate.describe(
                                    "com.pickme.orchestration.port의 인터페이스",
                                    javaClass -> javaClass.getPackageName()
                                            .startsWith("com.pickme.orchestration.port")
                            )
                    )
                    .because("CommandAdapter는 orchestration-api의 CommandPort 인터페이스를 "
                            + "구현하여 Temporal Activity와 도메인을 분리해야 한다.");

    // ─────────────────────────────────────────────────────────────────────
    // Workflow 격리 + 결정성 (Phase 5 추가)
    // ─────────────────────────────────────────────────────────────────────

    /** 결정성을 깨는 시간 / 난수 / Sleep API 호출. Workflow Replay 시 실행 결과가 달라질 수 있다. */
    private static final DescribedPredicate<JavaCall<?>> NON_DETERMINISTIC_API_CALL =
            new DescribedPredicate<JavaCall<?>>("결정성을 깨는 시간 / 난수 / Sleep API 호출") {
                @Override
                public boolean test(JavaCall<?> call) {
                    String name = call.getTarget().getName();
                    String owner = call.getTarget().getOwner().getFullName();

                    // java.time.* 의 .now() — LocalDateTime, Instant, ZonedDateTime, OffsetDateTime, LocalDate, LocalTime
                    if (name.equals("now") && owner.startsWith("java.time.")) return true;
                    // System.currentTimeMillis / nanoTime
                    if (owner.equals("java.lang.System")
                            && (name.equals("currentTimeMillis") || name.equals("nanoTime"))) return true;
                    // Math.random
                    if (owner.equals("java.lang.Math") && name.equals("random")) return true;
                    // Thread.sleep
                    if (owner.equals("java.lang.Thread") && name.equals("sleep")) return true;
                    // Random / ThreadLocalRandom 의 모든 호출
                    if (owner.equals("java.util.Random")
                            || owner.equals("java.util.concurrent.ThreadLocalRandom")) return true;
                    // UUID.randomUUID
                    if (owner.equals("java.util.UUID") && name.equals("randomUUID")) return true;

                    return false;
                }
            };

    @ArchTest
    static final ArchRule WorkflowInterface_구현체는_orchestration_workflow_패키지에만_위치한다 =
            classes()
                    .that().areAnnotatedWith("io.temporal.workflow.WorkflowInterface")
                    .should().resideInAPackage("..orchestration.workflow..")
                    .because("Temporal Workflow 인터페이스는 orchestration.workflow 하위에만 정의한다.");

    @ArchTest
    static final ArchRule Workflow_구현체는_비결정성_API를_사용하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..orchestration.workflow..")
                    .should().callMethodWhere(NON_DETERMINISTIC_API_CALL)
                    .because("Workflow 코드는 결정성을 보장해야 한다 (Replay 안전성). "
                            + "시간 / 난수는 Workflow.currentTimeMillis() / Workflow.newRandom() 으로 대체하거나 "
                            + "Activity 로 위임한다. Sleep 은 Workflow.sleep() 사용.");
}
