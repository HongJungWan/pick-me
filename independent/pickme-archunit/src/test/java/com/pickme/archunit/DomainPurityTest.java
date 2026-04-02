package com.pickme.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.pickme", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainPurityTest {

    @ArchTest
    static final ArchRule 도메인은_인프라스트럭처를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule 도메인은_API_계층을_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..api..");

    @ArchTest
    static final ArchRule 도메인은_스프링_프레임워크에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "org.springframework.boot..",
                            "org.springframework.web.."
                    );

    @ArchTest
    static final ArchRule 도메인은_JPA_어노테이션에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "javax.persistence..");
}
