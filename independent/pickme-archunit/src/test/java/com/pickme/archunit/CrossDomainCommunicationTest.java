package com.pickme.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 도메인 간 통신 방향 규칙.
 *
 * <p>{@link ModuleBoundaryTest}가 모든 도메인 간 상호 의존을 금지하므로 본 규칙은
 * 의미적으로 일부 중복되지만, <b>"상위 도메인이 하위 도메인을 의존하지 않는다"는
 * DDD 원칙을 명시적으로 문서화</b>하여 향후 peer 격리 정책이 완화되더라도
 * 상위→하위 방향 금지는 독립적으로 유지되도록 한다.</p>
 *
 * <h3>도메인 계층</h3>
 * <ul>
 *   <li><b>상위 (Upper, orchestration 주체)</b>: order, payment, settlement</li>
 *   <li><b>하위 (Lower, supporting)</b>: product, inventory, member, partner, notification</li>
 * </ul>
 *
 * <p>모든 cross-domain 통신은 다음 두 경로 중 하나로만 이루어져야 한다:</p>
 * <ol>
 *   <li>{@code common:pickme-orchestration-api}의 CommandPort 인터페이스 (동기 호출)</li>
 *   <li>{@code common:pickme-common}의 도메인 이벤트 (비동기 발행/구독)</li>
 * </ol>
 */
@ArchTestBase
class CrossDomainCommunicationTest {

    @ArchTest
    static final ArchRule 상위_도메인은_하위_도메인을_직접_의존하지_않는다 =
            noClasses()
                    .that().resideInAnyPackage(DomainModules.upperPackageMatchers())
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(DomainModules.lowerPackageMatchers())
                    .because("상위 도메인(order/payment/settlement)은 하위 도메인"
                            + "(product/inventory/member/partner/notification)을 직접 호출하지 않는다. "
                            + "통신은 orchestration-api의 CommandPort 또는 도메인 이벤트로만 한다.");

    @ArchTest
    static final ArchRule 하위_도메인은_상위_도메인을_직접_의존하지_않는다 =
            noClasses()
                    .that().resideInAnyPackage(DomainModules.lowerPackageMatchers())
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(DomainModules.upperPackageMatchers())
                    .because("하위 도메인은 상위 도메인을 알지 못해야 한다 (의존성 역전 원칙). "
                            + "하위 도메인은 자신의 도메인 이벤트만 발행하고, 상위가 구독한다.");
}
