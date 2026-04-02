package com.pickme.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.pickme", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    private static final String[] DOMAIN_MODULES = {
            "order", "payment", "product", "inventory",
            "member", "partner", "notification", "settlement"
    };

    @ArchTest
    static final ArchRule order_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..order..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..payment..", "..product..", "..inventory..",
                            "..member..", "..partner..", "..notification..", "..settlement.."
                    );

    @ArchTest
    static final ArchRule payment_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..payment..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..order..", "..product..", "..inventory..",
                            "..member..", "..partner..", "..notification..", "..settlement.."
                    );

    @ArchTest
    static final ArchRule product_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..product..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..order..", "..payment..", "..inventory..",
                            "..member..", "..partner..", "..notification..", "..settlement.."
                    );

    @ArchTest
    static final ArchRule inventory_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..inventory..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..order..", "..payment..", "..product..",
                            "..member..", "..partner..", "..notification..", "..settlement.."
                    );

    @ArchTest
    static final ArchRule member_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..member..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..order..", "..payment..", "..product..", "..inventory..",
                            "..partner..", "..notification..", "..settlement.."
                    );

    @ArchTest
    static final ArchRule partner_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..partner..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..order..", "..payment..", "..product..", "..inventory..",
                            "..member..", "..notification..", "..settlement.."
                    );

    @ArchTest
    static final ArchRule notification_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..notification..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..order..", "..payment..", "..product..", "..inventory..",
                            "..member..", "..partner..", "..settlement.."
                    );

    @ArchTest
    static final ArchRule settlement_모듈은_다른_도메인_모듈_내부를_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..settlement..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..order..", "..payment..", "..product..", "..inventory..",
                            "..member..", "..partner..", "..notification.."
                    );
}
