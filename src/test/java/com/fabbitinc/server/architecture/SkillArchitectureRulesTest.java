package com.fabbitinc.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AnalyzeClasses(
        packages = "com.fabbitinc.server",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class SkillArchitectureRulesTest {

    private static final Pattern APPLICATION_SERVICE_PACKAGE = Pattern.compile(
            "^com\\.fabbitinc\\.server\\.application\\.([^.]+)\\.service(?:\\..*)?$"
    );
    private static final Pattern APPLICATION_USECASE_PACKAGE = Pattern.compile(
            "^com\\.fabbitinc\\.server\\.application\\.([^.]+)\\.usecase(?:\\..*)?$"
    );
    private static final Pattern APPLICATION_QUERY_PACKAGE = Pattern.compile(
            "^com\\.fabbitinc\\.server\\.application\\.([^.]+)\\.query(?:\\..*)?$"
    );
    private static final Pattern APPLICATION_IMPLEMENTATION_PACKAGE = Pattern.compile(
            "^com\\.fabbitinc\\.server\\.application\\.([^.]+)\\.(service|usecase|query)(?:\\..*)?$"
    );
    private static final Pattern DOMAIN_REPOSITORY_PACKAGE = Pattern.compile(
            "^com\\.fabbitinc\\.server\\.domain\\.([^.]+)\\.repository(?:\\..*)?$"
    );

//    @ArchTest
//    static final ArchRule serviceClassesMustNotDependOnOtherDomainServiceOrRepository =
//            classes()
//                    .that().haveSimpleNameEndingWith("Service")
//                    .and().resideInAPackage("..application..service..")
//                    .should(notDependOnOtherDomainServiceOrRepository())
//                    .allowEmptyShould(true);
//
//    @ArchTest
//    static final ArchRule useCaseClassesMustNotDependOnOtherDomainImplementationOrRepository =
//            classes()
//                    .that().haveSimpleNameEndingWith("UseCase")
//                    .and().resideInAPackage("..application..usecase..")
//                    .should(notDependOnOtherDomainImplementationOrRepository())
//                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoryClassesMustNotDependOnOtherDomainRepositoryOrApplicationImplementation =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .and().resideInAPackage("..domain..repository..")
                    .should(notDependOnOtherDomainRepositoryOrApplicationImplementation())
                    .allowEmptyShould(true);

    private static ArchCondition<JavaClass> notDependOnOtherDomainServiceOrRepository() {
        return new ArchCondition<>("타 도메인의 Service/Repository 구현에 직접 의존하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String ownerDomain = extractDomain(clazz.getPackageName(), APPLICATION_SERVICE_PACKAGE);
                if (ownerDomain == null) {
                    return;
                }

                for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetPackage = targetClass.getPackageName();

                    String targetServiceDomain = extractDomain(targetPackage, APPLICATION_SERVICE_PACKAGE);
                    if (targetServiceDomain != null
                            && targetClass.getSimpleName().endsWith("Service")
                            && !ownerDomain.equals(targetServiceDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "타 도메인 Service")
                        ));
                    }

                    String targetRepositoryDomain = extractDomain(targetPackage, DOMAIN_REPOSITORY_PACKAGE);
                    if (targetRepositoryDomain != null
                            && targetClass.getSimpleName().endsWith("Repository")
                            && !ownerDomain.equals(targetRepositoryDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "타 도메인 Repository")
                        ));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnOtherDomainRepositoryOrApplicationImplementation() {
        return new ArchCondition<>("타 도메인 Repository 및 application 구현 계층에 직접 의존하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String ownerDomain = extractDomain(clazz.getPackageName(), DOMAIN_REPOSITORY_PACKAGE);
                if (ownerDomain == null) {
                    return;
                }

                for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetPackage = targetClass.getPackageName();

                    String targetRepositoryDomain = extractDomain(targetPackage, DOMAIN_REPOSITORY_PACKAGE);
                    if (targetRepositoryDomain != null
                            && targetClass.getSimpleName().endsWith("Repository")
                            && !ownerDomain.equals(targetRepositoryDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "타 도메인 Repository")
                        ));
                    }

                    Matcher applicationImplementation = APPLICATION_IMPLEMENTATION_PACKAGE.matcher(targetPackage);
                    if (applicationImplementation.matches()
                            && (
                            targetClass.getSimpleName().endsWith("Service")
                                    || targetClass.getSimpleName().endsWith("UseCase")
                                    || targetClass.getSimpleName().endsWith("Query")
                    )) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "application 구현 계층")
                        ));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnOtherDomainImplementationOrRepository() {
        return new ArchCondition<>("타 도메인 UseCase/Query/Service/Repository 구현에 직접 의존하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String ownerDomain = extractDomain(clazz.getPackageName(), APPLICATION_USECASE_PACKAGE);
                if (ownerDomain == null) {
                    return;
                }

                for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetPackage = targetClass.getPackageName();

                    String targetServiceDomain = extractDomain(targetPackage, APPLICATION_SERVICE_PACKAGE);
                    if (targetServiceDomain != null
                            && targetClass.getSimpleName().endsWith("Service")
                            && !ownerDomain.equals(targetServiceDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "타 도메인 Service")
                        ));
                    }

                    String targetUseCaseDomain = extractDomain(targetPackage, APPLICATION_USECASE_PACKAGE);
                    if (targetUseCaseDomain != null
                            && targetClass.getSimpleName().endsWith("UseCase")
                            && !ownerDomain.equals(targetUseCaseDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "타 도메인 UseCase")
                        ));
                    }

                    String targetQueryDomain = extractDomain(targetPackage, APPLICATION_QUERY_PACKAGE);
                    if (targetQueryDomain != null
                            && targetClass.getSimpleName().endsWith("Query")
                            && !ownerDomain.equals(targetQueryDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "타 도메인 Query")
                        ));
                    }

                    String targetRepositoryDomain = extractDomain(targetPackage, DOMAIN_REPOSITORY_PACKAGE);
                    if (targetRepositoryDomain != null
                            && targetClass.getSimpleName().endsWith("Repository")
                            && !ownerDomain.equals(targetRepositoryDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                dependency,
                                violationMessage(clazz, targetClass, "타 도메인 Repository")
                        ));
                    }
                }
            }
        };
    }

    private static String extractDomain(String packageName, Pattern pattern) {
        Matcher matcher = pattern.matcher(packageName);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1);
    }

    private static String violationMessage(JavaClass owner, JavaClass target, String dependencyType) {
        return "%s -> %s (%s 직접 참조 금지)"
                .formatted(owner.getName(), target.getName(), dependencyType);
    }
}
