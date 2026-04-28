package com.acme.cicd.product;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final JavaClasses classes =
      new ClassFileImporter().importPackages("com.acme.cicd.product");

  @Test
  void domainShouldNotDependOnApplication() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.acme.cicd.product.domain")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.acme.cicd.product.application");

    rule.check(classes);
  }

  @Test
  void domainShouldNotDependOnInfrastructure() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.acme.cicd.product.domain")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.acme.cicd.product.infrastructure");

    rule.check(classes);
  }

  @Test
  void domainShouldNotUseSpring() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.acme.cicd.product.domain")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework");

    rule.check(classes);
  }

  @Test
  void domainShouldNotUseJpa() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.acme.cicd.product.domain")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("jakarta.persistence");

    rule.check(classes);
  }

  @Test
  void applicationShouldNotDependOnInfrastructure() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.acme.cicd.product.application")
            .and()
            .resideOutsideOfPackage("com.acme.cicd.product.application.port.out")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.acme.cicd.product.infrastructure");

    rule.check(classes);
  }

  @Test
  void applicationShouldNotUseSpringStereotypes() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.acme.cicd.product.application")
            .and()
            .resideOutsideOfPackage("com.acme.cicd.product.application.port")
            .should()
            .beAnnotatedWith("org.springframework.stereotype.Service");

    rule.check(classes);
  }

  @Test
  void controllersShouldResideInInfrastructureWeb() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("com.acme.cicd.product.infrastructure.web")
            .and()
            .haveNameMatching(".*Controller")
            .should()
            .beAnnotatedWith("org.springframework.web.bind.annotation.RestController");

    rule.check(classes);
  }

  @Test
  void persistenceAdaptersShouldResideInInfrastructure() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("com.acme.cicd.product.infrastructure.persistence..")
            .should()
            .resideInAPackage("com.acme.cicd.product.infrastructure.persistence..");

    rule.check(classes);
  }
}
