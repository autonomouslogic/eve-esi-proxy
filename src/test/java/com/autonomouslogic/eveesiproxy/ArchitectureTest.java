package com.autonomouslogic.eveesiproxy;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
	static JavaClasses proxyClasses;

	@BeforeAll
	public static void beforeAll() {
		proxyClasses = new ClassFileImporter().importPackages(EveEsiProxy.class.getPackageName());
	}

	@Test
	public void noRootClasses() {
		noClasses()
				.that()
				.haveSimpleNameNotEndingWith("Test")
				.and()
				.haveSimpleNameNotStartingWith(EveEsiProxy.class.getSimpleName())
				.should()
				.resideInAPackage(EveEsiProxy.class.getPackageName())
				.check(proxyClasses);
	}

	@Test
	void javaxInjectShouldNotBeUsedOnClasses() {
		classes().should().notBeAnnotatedWith(javax.inject.Singleton.class).check(proxyClasses);
	}

	@Test
	void javaxInjectShouldNotBeUsedConstructors() {
		noConstructors().should().beAnnotatedWith(javax.inject.Inject.class).check(proxyClasses);
	}

	@Test
	void javaxInjectShouldNotBeUsedOnFields() {
		noFields().should().beAnnotatedWith(javax.inject.Inject.class).check(proxyClasses);
	}

	@Test
	void injectedClassesShouldHaveProtectedConstructors() {
		constructors()
				.that()
				.areAnnotatedWith(Inject.class)
				.should()
				.beProtected()
				.check(proxyClasses);
	}

	@Test
	void injectedClassesShouldNotHaveExtraConstructors() {
		constructors()
				.that()
				.areDeclaredInClassesThat()
				.containAnyConstructorsThat(CanBeAnnotated.Predicates.annotatedWith(Inject.class))
				.should()
				.beAnnotatedWith(Inject.class)
				.check(proxyClasses);
	}
}
