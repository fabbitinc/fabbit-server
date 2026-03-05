package com.fabbitinc.server.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.fabbitinc.server",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class LayerArchitectureRulesTest {

    @ArchTest
    static final ArchRule layeredDependencyRule =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .withOptionalLayers(true)
                    .layer("Presentation").definedBy("..presentation..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Infrastructure").definedBy("..infrastructure..")
                    .whereLayer("Presentation").mayOnlyAccessLayers("Application", "Domain")
                    .whereLayer("Application").mayOnlyAccessLayers("Domain")
                    .whereLayer("Infrastructure").mayOnlyAccessLayers("Application", "Domain")
                    .whereLayer("Domain").mayNotAccessAnyLayer();

    @ArchTest
    static final ArchRule controllerMustNotDependOnServiceOrRepository =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAnyPackage("..service..", "..repository..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule eventHandlerMustNotDependOnServiceRepositoryQueryOrController =
            noClasses()
                    .that().resideInAPackage("..eventhandler..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..service..",
                            "..repository..",
                            "..query..",
                            "..controller.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule queryMustNotDependOnUseCaseOrService =
            noClasses()
                    .that().resideInAPackage("..query..")
                    .should().dependOnClassesThat().resideInAnyPackage("..usecase..", "..service..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoryMustNotDependOnControllerUseCaseOrQuery =
            noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..usecase..", "..query..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule useCaseMustNotDependOnQuery =
            noClasses()
                    .that().resideInAPackage("..usecase..")
                    .should().dependOnClassesThat().resideInAPackage("..query..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllerMustNotDependOnDomainModelPackage =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..model..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllerMustNotExposeJpaEntityInMethodSignature =
            classes()
                    .that().resideInAPackage("..controller..")
                    .should(notExposeJpaEntityInMethodSignature())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule authUseCaseMustNotDependOnRequestResponseDto =
            noClasses()
                    .that().resideInAPackage("..application.auth.usecase..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application..dto.request..",
                            "..application..dto.response.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule authServiceMustNotDependOnRequestResponseDto =
            noClasses()
                    .that().resideInAPackage("..application.auth.service..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application..dto.request..",
                            "..application..dto.response.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule authQueryMustNotDependOnRequestResponseDto =
            noClasses()
                    .that().resideInAPackage("..application.auth.query..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application..dto.request..",
                            "..application..dto.response.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule authUseCaseClassMustHaveTransactional =
            classes()
                    .that().resideInAPackage("..application.auth.usecase..")
                    .and().haveSimpleNameEndingWith("UseCase")
                    .should().beAnnotatedWith(Transactional.class)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule authUseCaseMethodMustNotHaveTransactional =
            classes()
                    .that().resideInAPackage("..application.auth.usecase..")
                    .and().haveSimpleNameEndingWith("UseCase")
                    .should(notHaveMethodLevelTransactional())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule authQueryClassMustHaveReadOnlyTransactional =
            classes()
                    .that().resideInAPackage("..application.auth.query..")
                    .and().haveSimpleNameEndingWith("Query")
                    .should(haveClassLevelReadOnlyTransactional())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule authQueryMethodMustNotHaveTransactional =
            classes()
                    .that().resideInAPackage("..application.auth.query..")
                    .and().haveSimpleNameEndingWith("Query")
                    .should(notHaveMethodLevelTransactional())
                    .allowEmptyShould(true);

    private static ArchCondition<JavaClass> notExposeJpaEntityInMethodSignature() {
        return new ArchCondition<>("메서드 시그니처에서 JPA Entity를 노출하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getOwner().equals(clazz)) {
                        continue;
                    }

                    JavaClass returnType = method.getRawReturnType();
                    if (isJpaEntity(returnType)) {
                        String message = String.format(
                                "%s#%s return type %s 는 Entity 노출입니다",
                                clazz.getName(),
                                method.getName(),
                                returnType.getName()
                        );
                        events.add(SimpleConditionEvent.violated(method, message));
                    }

                    for (JavaClass parameterType : method.getRawParameterTypes()) {
                        if (!isJpaEntity(parameterType)) {
                            continue;
                        }
                        String message = String.format(
                                "%s#%s parameter type %s 는 Entity 노출입니다",
                                clazz.getName(),
                                method.getName(),
                                parameterType.getName()
                        );
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notHaveMethodLevelTransactional() {
        return new ArchCondition<>("메서드 레벨 @Transactional을 선언하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getOwner().equals(clazz)) {
                        continue;
                    }

                    if (!method.isAnnotatedWith(Transactional.class)) {
                        continue;
                    }

                    String message = String.format(
                            "%s#%s 는 메서드 레벨 @Transactional을 선언하고 있습니다",
                            clazz.getName(),
                            method.getName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> haveClassLevelReadOnlyTransactional() {
        return new ArchCondition<>("클래스 레벨 @Transactional(readOnly = true)를 선언한다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                boolean annotated = clazz.isAnnotatedWith(Transactional.class);
                boolean valid = false;
                if (annotated) {
                    Transactional annotation = clazz.getAnnotationOfType(Transactional.class);
                    valid = annotation.readOnly();
                }
                if (valid) {
                    return;
                }

                String message = String.format(
                        "%s 는 클래스 레벨 @Transactional(readOnly = true)가 필요합니다",
                        clazz.getName()
                );
                events.add(SimpleConditionEvent.violated(clazz, message));
            }
        };
    }

    private static boolean isJpaEntity(JavaClass javaClass) {
        return javaClass.isAnnotatedWith("jakarta.persistence.Entity")
                || javaClass.isAnnotatedWith("javax.persistence.Entity");
    }
}
