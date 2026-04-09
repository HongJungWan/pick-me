package com.pickme.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.pickme", importOptions = ImportOption.DoNotIncludeTests.class)
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
}
