package com.pickme.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@ArchTestBase
class TemporalIsolationTest {

    @ArchTest
    static final ArchRule temporal_SDK는_orchestration_모듈에서만_사용한다 =
            noClasses()
                    .that().resideOutsideOfPackage("..orchestration..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("io.temporal..");

    @ArchTest
    static final ArchRule 도메인_모듈은_orchestration_구현_세부사항에_의존하지_않는다 =
            noClasses()
                    .that().resideInAnyPackage(
                            "..order.domain..", "..payment.domain..", "..inventory.domain..",
                            "..product.domain..", "..member.domain..", "..partner.domain..",
                            "..notification.domain..", "..settlement.domain.."
                    )
                    .should().dependOnClassesThat()
                    .resideInAPackage("..orchestration..");
}
