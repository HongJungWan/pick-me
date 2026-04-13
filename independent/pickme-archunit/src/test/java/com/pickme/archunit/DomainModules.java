package com.pickme.archunit;

import java.util.List;

/**
 * pick-me 프로젝트의 DDD 도메인 모듈 이름을 한 곳에서 관리한다.
 *
 * <p>ArchUnit 규칙에서 도메인 모듈 목록을 하드코딩하지 않고 본 상수를 참조하도록 한다.
 * 새 도메인 모듈 추가 시 {@link #NAMES} 한 곳만 수정하면 전체 규칙에 자동으로 반영된다.</p>
 *
 * <h3>상위(Upper) / 하위(Lower) 도메인 구분</h3>
 * <ul>
 *   <li><b>Upper</b> (orchestration 주체): order, payment, settlement — 프로세스/오케스트레이션 성격</li>
 *   <li><b>Lower</b> (supporting): product, inventory, member, partner, notification — 기반 도메인</li>
 * </ul>
 *
 * <p>DDD 원칙상 상위 도메인이 하위 도메인 내부를 직접 참조해서는 안 된다.
 * 모든 cross-domain 통신은 {@code common:pickme-orchestration-api}의 CommandPort
 * 또는 {@code common:pickme-common}의 도메인 이벤트를 경유해야 한다.</p>
 */
final class DomainModules {

    private DomainModules() {
    }

    /** 전체 도메인 모듈 이름. 신규 모듈 추가 시 본 목록만 갱신한다. */
    static final List<String> NAMES = List.of(
            "order",
            "payment",
            "product",
            "inventory",
            "member",
            "partner",
            "notification",
            "settlement"
    );

    /** 상위(orchestration 주체) 도메인. */
    static final List<String> UPPER = List.of("order", "payment", "settlement");

    /** 하위(supporting) 도메인. */
    static final List<String> LOWER = List.of("product", "inventory", "member", "partner", "notification");

    /** {@code "..{module}.."} 형태의 패키지 매처 배열을 생성한다. */
    static String[] packageMatchers() {
        return NAMES.stream()
                .map(name -> ".." + name + "..")
                .toArray(String[]::new);
    }

    /** {@code "..{module}.{sub}.."} 형태의 하위 패키지 매처 배열을 생성한다. */
    static String[] subPackageMatchers(String subPackage) {
        return NAMES.stream()
                .map(name -> ".." + name + "." + subPackage + "..")
                .toArray(String[]::new);
    }

    /** 상위 도메인 패키지 매처 배열. */
    static String[] upperPackageMatchers() {
        return UPPER.stream()
                .map(name -> ".." + name + "..")
                .toArray(String[]::new);
    }

    /** 하위 도메인 패키지 매처 배열. */
    static String[] lowerPackageMatchers() {
        return LOWER.stream()
                .map(name -> ".." + name + "..")
                .toArray(String[]::new);
    }
}
