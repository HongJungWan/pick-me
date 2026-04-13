package com.pickme.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 도메인을 알지 말아야 할 공용/인프라 모듈이 도메인 모듈을 import 하지 못하도록 강제.
 *
 * <h3>대상 모듈</h3>
 * <ul>
 *   <li><b>공용/API</b>: {@code common:pickme-common}, {@code common:pickme-orchestration-api}</li>
 *   <li><b>인프라</b>: {@code application:pickme-gateway}, {@code application:pickme-config-server}, {@code application:pickme-discovery}</li>
 * </ul>
 *
 * <p>이들 모듈이 도메인을 의존하면 다음 문제가 생긴다.</p>
 * <ul>
 *   <li>{@code common} → 도메인 의존: 모든 도메인이 common 을 의존하므로 순환 의존 발생</li>
 *   <li>{@code orchestration-api} → 도메인 의존: 포트 인터페이스가 도메인 구현 세부에 묶여
 *       Temporal Activity 격리 의도가 무너짐</li>
 *   <li>{@code gateway/config/discovery} → 도메인 의존: 네트워크/인프라 모듈이 비즈니스 로직을 알게 되어
 *       모듈러 모놀리스 경계가 깨짐</li>
 * </ul>
 *
 * <p>현재 위반 0건. 회귀 방지를 위해 엄격(freeze 미적용) 적용.</p>
 */
@ArchTestBase
class SharedModuleIsolationTest {

    @ArchTest
    static final ArchRule 공용_및_API_모듈은_도메인_모듈을_의존하지_않는다 =
            noClasses()
                    .that().resideInAnyPackage(
                            "com.pickme.common..",
                            "com.pickme.orchestration.port..",
                            "com.pickme.orchestration.dto..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(DomainModules.packageMatchers())
                    .because("공용 인프라(common)와 orchestration-api 는 어떤 도메인도 알지 못해야 한다. "
                            + "도메인이 이들을 의존하는 방향만 허용된다.");

    @ArchTest
    static final ArchRule 인프라_모듈_gateway_config_discovery는_도메인_모듈을_의존하지_않는다 =
            noClasses()
                    .that().resideInAnyPackage(
                            "com.pickme.gateway..",
                            "com.pickme.config..",
                            "com.pickme.discovery..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(DomainModules.packageMatchers())
                    .because("Gateway / Config Server / Eureka 는 네트워크·인프라 책임만 가지며 "
                            + "도메인 비즈니스 로직을 알지 않는다.");
}
