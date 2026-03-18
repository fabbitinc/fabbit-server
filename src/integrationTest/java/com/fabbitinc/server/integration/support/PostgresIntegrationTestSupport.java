package com.fabbitinc.server.integration.support;

import com.fabbitinc.server.ServerApplication;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(classes = ServerApplication.class)
@Import(SubscriptionIntegrationTestConfig.class)
public abstract class PostgresIntegrationTestSupport {

    protected static final PostgreSQLContainer<?> POSTGRESQL;

    static {
        POSTGRESQL = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRESQL.start();
    }

    @Autowired
    protected TestCurrentAuthProvider testCurrentAuthProvider;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRESQL::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @AfterEach
    void clearTestContexts() {
        testCurrentAuthProvider.clear();
        TenantContextHolder.clear();
    }
}
