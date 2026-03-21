import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.2.1"
}

group = "com.fabbitinc"
version = "0.0.1-SNAPSHOT"
description = "server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

springBoot {
    mainClass.set("com.fabbitinc.server.ServerApplication")
}

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:1.0.3"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.37")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
    implementation("com.github.f4b6a3:uuid-creator:6.1.1")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("software.amazon.awssdk:s3:2.25.53")
    implementation("org.apache.poi:poi-ooxml:5.4.0")
    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.4")
    implementation("com.twelvemonkeys.imageio:imageio-core:3.12.0")
    implementation("com.twelvemonkeys.imageio:imageio-tiff:3.12.0")
    runtimeOnly("com.github.usefulness:webp-imageio:0.10.1")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("io.github.openfeign.querydsl:querydsl-core:7.1")
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        resources {
            srcDir("migrations")
        }
    }
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}

configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.register<Test>("integrationTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs integration tests."
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
}

tasks.register<Test>("archTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs ArchUnit architecture tests only."
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter {
        includeTestsMatching("com.fabbitinc.server.architecture.*Test")
    }
}

spotless {
    java {
        target("src/*/java/**/*.java")
        // googleJavaFormat()
        // trimTrailingWhitespace()
        // endWithNewline()
        importOrder()
        removeUnusedImports()
        formatAnnotations()
    }
}

tasks.register<JavaExec>("schemaExport") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.fabbitinc.server.tools.SchemaExporter")
}

tasks.register<JavaExec>("schemaExportPublic") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.fabbitinc.server.tools.SchemaExporter")
    args = listOf("public")
}

tasks.register<JavaExec>("schemaExportTenant") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.fabbitinc.server.tools.SchemaExporter")
    args = listOf("tenant")
}

tasks.named<BootRun>("bootRun") {
    val envFile = file(".env")
    if (envFile.exists()) {
        envFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.split("=", limit = 2) }
            .filter { it.size == 2 }
            .forEach { (key, value) -> environment(key, value) }
    }
}
