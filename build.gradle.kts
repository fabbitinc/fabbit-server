plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.fabbitinc"
version = "0.0.1-SNAPSHOT"
description = "server2"

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

dependencies {
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
    implementation("com.github.f4b6a3:uuid-creator:6.1.1")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("software.amazon.awssdk:s3:2.25.53")
    implementation("org.apache.poi:poi-ooxml:5.4.0")
    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation("io.github.openfeign.querydsl:querydsl-core:7.1")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        resources {
            srcDir("migrations")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
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
