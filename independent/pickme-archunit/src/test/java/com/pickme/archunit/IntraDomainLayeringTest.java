package com.pickme.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 도메인 모듈 내부 레이어드 아키텍처 규칙.
 *
 * <h3>계층 의존 방향</h3>
 * <pre>
 *   API             ──▶ Application, Domain
 *   Application     ──▶ Domain
 *   Infrastructure  ──▶ Application, Domain     (Kafka 컨슈머가 핸들러 호출)
 *   Domain          ──▶ (어떤 계층도 참조 금지)
 * </pre>
 *
 * <p><b>왜 Infrastructure → Application을 허용하는가?</b><br>
 * 도메인 이벤트 컨슈머({@code OrderSagaConsumer}, {@code SettlementEventConsumer} 등)가
 * {@code application.*EventHandler}를 호출하는 driven adapter 패턴이 일반적이기 때문이다.
 * 이는 의존성 역전을 의도적으로 한쪽 방향으로 풀어주는 정상 패턴이다.</p>
 *
 * <h3>FreezingArchRule 적용 사유</h3>
 * <p>현재 {@code application → infrastructure} 위반이 13건 존재한다
 * (snapshot, JwtProvider, StockRedisService 등 의도적 설계). 이를 즉시 리팩터링하지
 * 않고 {@link FreezingArchRule#freeze}로 베이스라인에 기록하여 <b>신규 위반만 차단</b>한다.
 * 베이스라인은 {@code archunit_store/} 디렉토리에 저장되며 git에 커밋되어
 * 모든 개발자/CI가 같은 기준을 공유한다.</p>
 *
 * <p>위반을 해소하면 {@code archunit.properties}의 {@code freeze.refreeze=true}로
 * 일시 변경 후 테스트를 1회 실행하여 store를 갱신한다.</p>
 */
@ArchTestBase
class IntraDomainLayeringTest {

    /** 단일 도메인 모듈에 대한 레이어드 아키텍처 규칙을 생성한다. */
    private static ArchRule layeringRuleFor(String module) {
        String base = "com.pickme." + module;
        return Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage(base + "..")
                .layer("API").definedBy(base + ".api..")
                .layer("Application").definedBy(base + ".application..")
                .layer("Domain").definedBy(base + ".domain..")
                .layer("Infrastructure").definedBy(base + ".infrastructure..")
                .whereLayer("API").mayOnlyAccessLayers("Application", "Domain")
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Application", "Domain")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .as(module + " 도메인 내부 계층 의존성 규칙")
                .because("DDD 계층 규칙: Domain은 외부 계층 참조 금지, "
                        + "Application은 Infrastructure를 직접 사용하지 않는다.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 1. 레이어드 아키텍처 (도메인별 8개) — 기존 위반은 freeze로 베이스라인 처리
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule order_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("order"));

    @ArchTest
    static final ArchRule payment_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("payment"));

    @ArchTest
    static final ArchRule product_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("product"));

    @ArchTest
    static final ArchRule inventory_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("inventory"));

    @ArchTest
    static final ArchRule member_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("member"));

    @ArchTest
    static final ArchRule partner_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("partner"));

    @ArchTest
    static final ArchRule notification_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("notification"));

    @ArchTest
    static final ArchRule settlement_레이어드_규칙 = FreezingArchRule.freeze(layeringRuleFor("settlement"));

    // ─────────────────────────────────────────────────────────────────────
    // 2. Repository Port/Impl 위치 규칙 — 엄격 적용 (현재 위반 0건)
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule 도메인_Repository_인터페이스는_domain_repository_패키지에_있어야_한다 =
            classes()
                    .that().areInterfaces()
                    .and().haveSimpleNameEndingWith("Repository")
                    .and().resideInAPackage("..domain..")
                    .should().resideInAPackage("..domain.repository..")
                    .because("도메인 포트(Repository 인터페이스)는 domain.repository 하위에만 위치한다.");

    @ArchTest
    static final ArchRule Repository_구현체는_infrastructure_패키지에_있어야_한다 =
            classes()
                    .that().haveSimpleNameEndingWith("RepositoryImpl")
                    .should().resideInAPackage("..infrastructure..")
                    .because("Repository 구현체는 infrastructure 계층의 어댑터이다.");

    @ArchTest
    static final ArchRule Spring_Data_JpaRepository는_infrastructure_패키지에_있어야_한다 =
            classes()
                    .that().areInterfaces()
                    .and().haveSimpleNameStartingWith("Jpa")
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().resideInAPackage("..infrastructure..")
                    .because("Spring Data JPA Repository는 인프라 어댑터이며 도메인 계층에 노출되면 안 된다.");

    // ─────────────────────────────────────────────────────────────────────
    // 3. JPA @Entity 배치 규칙 — 엄격 적용 (현재 위반 0건)
    //    문자열 기반 어노테이션 매칭으로 jakarta.persistence 직접 의존을 회피한다.
    // ─────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule 도메인_모듈의_JPA_Entity는_infrastructure_persistence_또는_snapshot에만_위치한다 =
            classes()
                    .that().areAnnotatedWith("jakarta.persistence.Entity")
                    .and().resideInAnyPackage(DomainModules.packageMatchers())
                    .should().resideInAnyPackage(
                            "..infrastructure.persistence..",
                            "..infrastructure.snapshot.."
                    )
                    .because("도메인 모듈의 JPA Entity는 영속화 어댑터이며 도메인 모델과 분리되어야 한다. "
                            + "공용 인프라(common.outbox/dlt/idempotency)는 본 규칙 대상 외이다.");
}
