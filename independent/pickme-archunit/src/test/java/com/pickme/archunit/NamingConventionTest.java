package com.pickme.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 클래스 명명 및 위치 규칙.
 *
 * <p>모듈/계층/어노테이션과 클래스명 패턴이 일관되도록 강제하여 코드 탐색·리뷰 비용을 낮춘다.</p>
 */
@ArchTestBase
class NamingConventionTest {

    @ArchTest
    static final ArchRule Controller는_Controller_접미사를_가져야_한다 =
            classes()
                    .that().resideInAPackage("..api..")
                    .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule Service는_Service_또는_EventHandler_접미사를_가져야_한다 =
            classes()
                    .that().resideInAPackage("..application..")
                    .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("EventHandler")
                    .orShould().haveSimpleNameEndingWith("CommandAdapter");

    @ArchTest
    static final ArchRule EventHandler는_EventHandler_접미사를_가져야_한다 =
            classes()
                    .that().resideInAPackage("..application..")
                    .and().haveSimpleNameContaining("Event")
                    .should().haveSimpleNameEndingWith("EventHandler");

    // ─────────────────────────────────────────────────────────────────────
    // Phase 6 추가: @Configuration / *Gateway / 도메인 enum 위치 규칙
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule Configuration_클래스는_Config_또는_Configuration_접미사를_가져야_한다 =
            FreezingArchRule.freeze(
                    classes()
                            .that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                            .should().haveSimpleNameEndingWith("Config")
                            .orShould().haveSimpleNameEndingWith("Configuration")
                            .as("NAMING_CONFIG_SUFFIX — @Configuration 클래스는 Config / Configuration 접미사를 갖는다")
                            .because("설정 클래스는 명명만 보고 역할을 식별할 수 있어야 한다.")
            );

    @ArchTest
    static final ArchRule 도메인_모듈의_Gateway_구현체는_infrastructure_external에_위치한다 =
            classes()
                    .that().haveSimpleNameEndingWith("Gateway")
                    .and().areNotInterfaces()
                    .and().resideInAnyPackage(DomainModules.packageMatchers())
                    .should().resideInAPackage("..infrastructure.external..")
                    .because("외부 시스템 호출 어댑터(Gateway) 구현체는 infrastructure.external 하위에만 위치한다. "
                            + "단, 인터페이스(도메인 포트) 정의는 도메인 어디서나 가능.");

    @ArchTest
    static final ArchRule 도메인_모듈의_enum은_domain_model_패키지에만_위치한다 =
            classes()
                    .that().areEnums()
                    .and().resideInAnyPackage(DomainModules.subPackageMatchers("domain"))
                    .should().resideInAPackage("..domain.model..")
                    .because("도메인 enum 은 domain.model 하위에 모아 카탈로그화한다.");
}
