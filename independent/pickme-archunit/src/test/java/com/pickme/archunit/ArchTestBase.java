package com.pickme.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * pick-me ArchUnit 테스트 공용 분석 범위 설정 메타 어노테이션.
 *
 * <p>ArchUnit의 {@link AnalyzeClasses}는 {@code @Inherited}가 선언되어 있지만,
 * ArchUnit JUnit5 엔진이 클래스 계층 상속을 통해서는 해당 어노테이션을 인식하지 못한다
 * (ArchUnitTestDescriptor 기준 직접 선언된 어노테이션 또는 메타 어노테이션만 탐지).</p>
 *
 * <p>따라서 추상 상위 클래스 상속이 아닌 <b>메타 어노테이션 방식</b>으로
 * {@code @AnalyzeClasses} 설정을 재사용한다. 모든 ArchUnit 테스트 클래스는 본
 * {@code @ArchTestBase} 하나만 부착하면 된다.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@AnalyzeClasses(
        packages = "com.pickme",
        importOptions = ImportOption.DoNotIncludeTests.class
)
@interface ArchTestBase {
}
