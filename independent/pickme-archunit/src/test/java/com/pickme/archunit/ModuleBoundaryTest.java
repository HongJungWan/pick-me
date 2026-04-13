package com.pickme.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.Slice;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 도메인 모듈 간 상호 의존 금지 규칙.
 *
 * <p>각 도메인은 독립된 Bounded Context로서 다른 도메인 내부를 직접 참조해서는 안 된다.
 * Cross-domain 통신은 반드시 {@code common:pickme-orchestration-api}의 CommandPort
 * 또는 {@code common:pickme-common}의 도메인 이벤트를 경유해야 한다.</p>
 *
 * <p>구현 방식: {@code slices()}로 {@code com.pickme.(*)} 첫 세그먼트를 자동 추출한 뒤,
 * {@link DomainModules#NAMES}에 포함된 도메인만 필터링하여 상호 의존을 검증한다.
 * 신규 도메인 추가 시 {@link DomainModules} 한 곳만 수정하면 된다.</p>
 */
@ArchTestBase
class ModuleBoundaryTest {

    /** 현재 slice의 첫 번째 캡처 세그먼트가 도메인 모듈에 해당하는지 검사. */
    private static final DescribedPredicate<Slice> IS_DOMAIN_MODULE =
            new DescribedPredicate<Slice>("도메인 모듈에 해당") {
                @Override
                public boolean test(Slice slice) {
                    return DomainModules.NAMES.contains(slice.getNamePart(1));
                }
            };

    @ArchTest
    static final ArchRule 도메인_모듈은_서로_의존하지_않는다 =
            slices()
                    .matching("com.pickme.(*)..")
                    .that(IS_DOMAIN_MODULE)
                    .as("도메인 모듈(" + String.join(", ", DomainModules.NAMES) + ")")
                    .should().notDependOnEachOther()
                    .because("각 도메인은 독립된 Bounded Context이며, cross-domain 호출은 "
                            + "common:pickme-orchestration-api의 CommandPort 또는 "
                            + "common:pickme-common의 도메인 이벤트를 경유해야 한다.");
}
