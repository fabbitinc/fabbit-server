package com.fabbitinc.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AbstractIdEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@AnalyzeClasses(
        packages = "com.fabbitinc.server",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class EntityArchitectureRulesTest {

    private static final String UUID_CLASS_NAME = "java.util.UUID";
    private static final String RUNNABLE_CLASS_NAME = "java.lang.Runnable";
    private static final String JAVA_UTIL_FUNCTION_PACKAGE = "java.util.function";

    @ArchTest
    static final ArchRule entitiesMustExtendAbstractIdEntity =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().resideInAPackage("..domain..")
                    .should().beAssignableTo(AbstractIdEntity.class)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesMustNotExposePublicConstructors =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().resideInAPackage("..domain..")
                    .should(notHavePublicConstructors())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesMustDeclareProtectedNoArgsConstructor =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().resideInAPackage("..domain..")
                    .should(haveProtectedNoArgsConstructor())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesMustNotExposePublicSetters =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().resideInAPackage("..domain..")
                    .should(notHavePublicSetters())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesMustNotDependOnApplicationOrPresentation =
            noClasses()
                    .that().areAnnotatedWith(Entity.class)
                    .and().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application..",
                            "..presentation..",
                            "..dto.."
                    )
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule aggregateRootsMustBeEntitiesExtendingAbstractIdEntity =
            classes()
                    .that().implement(AggregateRoot.class)
                    .and().doNotHaveSimpleName("AggregateRoot")
                    .should().beAnnotatedWith(Entity.class)
                    .andShould().beAssignableTo(AbstractIdEntity.class)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule relationHelperFieldsMustUseReadOnlyRelationPattern =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().resideInAPackage("..domain..")
                    .should(haveValidReadOnlyRelationHelperFields())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule actorAwareEntitiesMustNotExposeActorlessConvenienceOverloads =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().areAssignableTo(AbstractActorAuditableEntity.class)
                    .should(notHaveActorlessConvenienceOverloads())
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule actorAwareEntitiesMustNotExposePublicCallbackParameters =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().areAssignableTo(AbstractActorAuditableEntity.class)
                    .should(notExposePublicCallbackParameters())
                    .allowEmptyShould(true);

    private static ArchCondition<JavaClass> notHavePublicConstructors() {
        return new ArchCondition<>("public 생성자를 노출하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaConstructor constructor : clazz.getConstructors()) {
                    if (!constructor.getOwner().equals(clazz)) {
                        continue;
                    }
                    if (!constructor.getModifiers().contains(JavaModifier.PUBLIC)) {
                        continue;
                    }

                    String message = String.format(
                            "%s 는 public 생성자 %s 를 노출하고 있습니다",
                            clazz.getName(),
                            constructor.getFullName()
                    );
                    events.add(SimpleConditionEvent.violated(constructor, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> haveProtectedNoArgsConstructor() {
        return new ArchCondition<>("protected no-args 생성자를 가진다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                boolean hasProtectedNoArgsConstructor = clazz.getConstructors().stream()
                        .filter(constructor -> constructor.getOwner().equals(clazz))
                        .filter(constructor -> constructor.getRawParameterTypes().isEmpty())
                        .anyMatch(constructor -> constructor.getModifiers().contains(JavaModifier.PROTECTED));

                if (hasProtectedNoArgsConstructor) {
                    return;
                }

                String message = String.format(
                        "%s 는 protected no-args 생성자가 필요합니다",
                        clazz.getName()
                );
                events.add(SimpleConditionEvent.violated(clazz, message));
            }
        };
    }

    private static ArchCondition<JavaClass> notHavePublicSetters() {
        return new ArchCondition<>("public setX 메서드를 노출하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                clazz.getMethods().stream()
                        .filter(method -> method.getOwner().equals(clazz))
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(method -> method.getName().startsWith("set"))
                        .filter(method -> method.getName().length() > 3)
                        .filter(method -> Character.isUpperCase(method.getName().charAt(3)))
                        .forEach(method -> {
                            String message = String.format(
                                    "%s#%s 는 public setX 메서드를 노출하고 있습니다",
                                    clazz.getName(),
                                    method.getName()
                            );
                            events.add(SimpleConditionEvent.violated(method, message));
                        });
            }
        };
    }

    private static ArchCondition<JavaClass> haveValidReadOnlyRelationHelperFields() {
        return new ArchCondition<>("_...Relation 필드는 읽기 전용 JPA 보조 relation 패턴을 따른다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaField field : clazz.getFields()) {
                    if (!field.getOwner().equals(clazz)) {
                        continue;
                    }
                    if (!field.getName().matches("^_[A-Za-z0-9]+Relation$")) {
                        continue;
                    }

                    validateRelationField(clazz, field, events);
                }
            }

            private void validateRelationField(JavaClass clazz, JavaField field, ConditionEvents events) {
                boolean association = field.isAnnotatedWith(ManyToOne.class) || field.isAnnotatedWith(OneToOne.class);
                if (!association) {
                    String message = String.format(
                            "%s.%s 는 @ManyToOne 또는 @OneToOne 이어야 합니다",
                            clazz.getName(),
                            field.getName()
                    );
                    events.add(SimpleConditionEvent.violated(field, message));
                    return;
                }

                if (!field.isAnnotatedWith(JoinColumn.class)) {
                    String message = String.format(
                            "%s.%s 는 @JoinColumn 이 필요합니다",
                            clazz.getName(),
                            field.getName()
                    );
                    events.add(SimpleConditionEvent.violated(field, message));
                    return;
                }

                JoinColumn joinColumn = field.getAnnotationOfType(JoinColumn.class);
                if (joinColumn.insertable() || joinColumn.updatable()) {
                    String message = String.format(
                            "%s.%s 는 insertable = false, updatable = false 여야 합니다",
                            clazz.getName(),
                            field.getName()
                    );
                    events.add(SimpleConditionEvent.violated(field, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notHaveActorlessConvenienceOverloads() {
        return new ArchCondition<>("actor UUID만 빠진 public convenience overload를 노출하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getOwner().equals(clazz)) {
                        continue;
                    }
                    if (!method.getModifiers().contains(JavaModifier.PUBLIC)) {
                        continue;
                    }
                    if (hasTrailingUuidParameter(method)) {
                        continue;
                    }

                    boolean hasActorAwareOverload = clazz.getMethods().stream()
                            .filter(candidate -> candidate.getOwner().equals(clazz))
                            .filter(candidate -> candidate.getModifiers().contains(JavaModifier.PUBLIC))
                            .filter(candidate -> candidate.getName().equals(method.getName()))
                            .filter(candidate -> candidate.getRawParameterTypes().size() == method.getRawParameterTypes().size() + 1)
                            .filter(EntityArchitectureRulesTest::hasTrailingUuidParameter)
                            .anyMatch(candidate -> hasSameLeadingParameterTypes(method, candidate));

                    if (!hasActorAwareOverload) {
                        continue;
                    }

                    String message = String.format(
                            "%s#%s 는 actor UUID만 빠진 public convenience overload를 노출하고 있습니다",
                            clazz.getName(),
                            method.getFullName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notExposePublicCallbackParameters() {
        return new ArchCondition<>("public 도메인 API에서 Runnable/Consumer 같은 콜백을 받지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getOwner().equals(clazz)) {
                        continue;
                    }
                    if (!method.getModifiers().contains(JavaModifier.PUBLIC)) {
                        continue;
                    }

                    for (JavaClass parameterType : method.getRawParameterTypes()) {
                        if (!isCallbackType(parameterType)) {
                            continue;
                        }

                        String message = String.format(
                                "%s#%s 는 public 도메인 API에서 콜백 파라미터 %s 를 노출하고 있습니다",
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

    private static boolean hasTrailingUuidParameter(JavaMethod method) {
        if (method.getRawParameterTypes().isEmpty()) {
            return false;
        }
        JavaClass lastParameterType = method.getRawParameterTypes().get(method.getRawParameterTypes().size() - 1);
        return UUID_CLASS_NAME.equals(lastParameterType.getName());
    }

    private static boolean hasSameLeadingParameterTypes(JavaMethod actorlessMethod, JavaMethod actorAwareMethod) {
        for (int index = 0; index < actorlessMethod.getRawParameterTypes().size(); index++) {
            if (!actorlessMethod.getRawParameterTypes().get(index).equals(actorAwareMethod.getRawParameterTypes().get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCallbackType(JavaClass parameterType) {
        if (RUNNABLE_CLASS_NAME.equals(parameterType.getName())) {
            return true;
        }
        return parameterType.getPackageName().startsWith(JAVA_UTIL_FUNCTION_PACKAGE);
    }
}
