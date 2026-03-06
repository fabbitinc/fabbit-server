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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.fabbitinc.server",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class ConventionArchitectureRulesTest {

    @ArchTest
    static final ArchRule queryClassesMustDeclareClassLevelReadOnlyTransactional =
            classes()
                    .that().haveSimpleNameEndingWith("Query")
                    .and().resideInAPackage("..query..")
                    .should(haveClassLevelReadOnlyTransactional())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule queryClassesMustNotDeclareMethodLevelTransactional =
            classes()
                    .that().haveSimpleNameEndingWith("Query")
                    .and().resideInAPackage("..query..")
                    .should(notHaveMethodLevelTransactional())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule queryClassesMustNotDependOnWebDtoOrPresentation =
            noClasses()
                    .that().haveSimpleNameEndingWith("Query")
                    .and().resideInAPackage("..query..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..dto.request..",
                            "..dto.response..",
                            "..presentation.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule useCaseClassesMustDeclareClassLevelTransactional =
            classes()
                    .that().haveSimpleNameEndingWith("UseCase")
                    .and().resideInAPackage("..usecase..")
                    .should(haveClassLevelTransactional())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule useCaseClassesMustNotDeclareMethodLevelTransactional =
            classes()
                    .that().haveSimpleNameEndingWith("UseCase")
                    .and().resideInAPackage("..usecase..")
                    .should(notHaveMethodLevelTransactional())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule useCaseClassesMustNotDependOnWebDtoOrPresentation =
            noClasses()
                    .that().haveSimpleNameEndingWith("UseCase")
                    .and().resideInAPackage("..usecase..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..dto.request..",
                            "..dto.response..",
                            "..presentation.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule serviceClassesMustNotDependOnWebDtoOrPresentation =
            noClasses()
                    .that().haveSimpleNameEndingWith("Service")
                    .and().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..dto.request..",
                            "..dto.response..",
                            "..presentation.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllerClassesMustBeAnnotatedWithTag =
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .and().resideInAPackage("..controller..")
                    .should().beAnnotatedWith(Tag.class)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllerEndpointMethodsMustBeAnnotatedWithOperation =
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .and().resideInAPackage("..controller..")
                    .should(haveOperationOnEndpointMethods())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllerClassesOrEndpointMethodsMustDeclareApiResponses =
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .and().resideInAPackage("..controller..")
                    .should(haveApiResponsesOnClassOrEndpointMethods())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllerEndpointMethodsMustNotReturnObject =
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .and().resideInAPackage("..controller..")
                    .should(notReturnObjectFromEndpointMethods())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule requestDtosMustBeAnnotatedWithSchema =
            classes()
                    .that().resideInAnyPackage("..application..dto..", "..presentation..dto..")
                    .and().haveSimpleNameEndingWith("Request")
                    .should(haveSchemaOnTypeOrMembers())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule responseDtosMustBeAnnotatedWithSchema =
            classes()
                    .that().resideInAnyPackage("..application..dto..", "..presentation..dto..")
                    .and().haveSimpleNameEndingWith("Response")
                    .should(haveSchemaOnTypeOrMembers())
                    .allowEmptyShould(true);

    private static ArchCondition<JavaClass> haveClassLevelTransactional() {
        return new ArchCondition<>("클래스 레벨 @Transactional을 선언한다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                if (clazz.isAnnotatedWith(Transactional.class)) {
                    return;
                }

                String message = String.format(
                        "%s 는 클래스 레벨 @Transactional이 필요합니다",
                        clazz.getName()
                );
                events.add(SimpleConditionEvent.violated(clazz, message));
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

    private static ArchCondition<JavaClass> haveOperationOnEndpointMethods() {
        return new ArchCondition<>("엔드포인트 메서드에 @Operation을 선언한다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getOwner().equals(clazz) || !isEndpointMethod(method)) {
                        continue;
                    }
                    if (method.isAnnotatedWith(Operation.class)) {
                        continue;
                    }

                    String message = String.format(
                            "%s#%s 는 @Operation이 필요합니다",
                            clazz.getName(),
                            method.getName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> haveApiResponsesOnClassOrEndpointMethods() {
        return new ArchCondition<>("컨트롤러 클래스 또는 엔드포인트 메서드에 @ApiResponses를 선언한다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                boolean classAnnotated = clazz.isAnnotatedWith(ApiResponses.class);
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getOwner().equals(clazz) || !isEndpointMethod(method)) {
                        continue;
                    }
                    if (classAnnotated || method.isAnnotatedWith(ApiResponses.class)) {
                        continue;
                    }

                    String message = String.format(
                            "%s#%s 는 @ApiResponses가 필요합니다",
                            clazz.getName(),
                            method.getName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notReturnObjectFromEndpointMethods() {
        return new ArchCondition<>("엔드포인트 메서드 반환 타입으로 Object를 사용하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getOwner().equals(clazz) || !isEndpointMethod(method)) {
                        continue;
                    }
                    if (!Object.class.getName().equals(method.getRawReturnType().getName())) {
                        continue;
                    }

                    String message = String.format(
                            "%s#%s 는 반환 타입으로 Object를 사용하고 있습니다",
                            clazz.getName(),
                            method.getName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> haveSchemaOnTypeOrMembers() {
        return new ArchCondition<>("타입 또는 멤버에 @Schema를 선언한다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                if (clazz.getName().contains("$")) {
                    return;
                }
                if (clazz.isAnnotatedWith(Schema.class)) {
                    return;
                }

                try {
                    Class<?> reflected = clazz.reflect();
                    if (reflected.isAnnotationPresent(Schema.class)) {
                        return;
                    }
                    for (java.lang.reflect.Field field : reflected.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Schema.class)) {
                            return;
                        }
                    }
                    if (reflected.isRecord()) {
                        for (java.lang.reflect.RecordComponent component : reflected.getRecordComponents()) {
                            if (component.isAnnotationPresent(Schema.class)) {
                                return;
                            }
                        }
                    }
                    for (java.lang.reflect.Method method : reflected.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Schema.class)) {
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }

                String message = String.format(
                        "%s 는 타입 또는 멤버에 @Schema가 필요합니다",
                        clazz.getName()
                );
                events.add(SimpleConditionEvent.violated(clazz, message));
            }
        };
    }

    private static boolean isEndpointMethod(JavaMethod method) {
        return method.isAnnotatedWith(GetMapping.class)
                || method.isAnnotatedWith(PostMapping.class)
                || method.isAnnotatedWith(PutMapping.class)
                || method.isAnnotatedWith(PatchMapping.class)
                || method.isAnnotatedWith(DeleteMapping.class)
                || method.isAnnotatedWith(RequestMapping.class);
    }
}
