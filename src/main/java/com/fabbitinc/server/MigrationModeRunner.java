package com.fabbitinc.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("migrate")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MigrationModeRunner implements ApplicationRunner {

    private static final String CHANGELOG_PATH = "classpath:migrations/changelog-master.xml";

    private final DataSource dataSource;
    private final ConfigurableApplicationContext applicationContext;
    private final ResourceLoader resourceLoader;

    @Value("${migration.target:all}")
    private String migrationTarget;

    @Value("${migration.tenant-schema:}")
    private String tenantSchema;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            String target = normalizeTarget(migrationTarget);
            log.info("migration mode started: target={}, tenantSchema={}", target, tenantSchema);

            if (target.equals("public") || target.equals("all")) {
                migratePublicSchema();
            }

            if (target.equals("tenant") || target.equals("all")) {
                migrateTenantSchemas();
            }

            log.info("migration mode completed successfully");
        } finally {
            applicationContext.close();
        }
    }

    private String normalizeTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return "all";
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "public", "tenant", "all" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported migration.target: " + raw);
        };
    }

    private void migratePublicSchema() throws Exception {
        log.info("applying public schema migrations");
        runLiquibase("public", "public");
        log.info("public schema migrations completed");
    }

    private void migrateTenantSchemas() throws Exception {
        List<String> schemas = resolveTenantSchemas();
        if (schemas.isEmpty()) {
            log.info("no tenant schemas found; skipping tenant migrations");
            return;
        }

        for (String schema : schemas) {
            log.info("applying tenant schema migrations: schema={}", schema);
            runLiquibase(schema, "tenant");
            log.info("tenant schema migrations completed: schema={}", schema);
        }
    }

    private List<String> resolveTenantSchemas() throws Exception {
        if (tenantSchema != null && !tenantSchema.isBlank()) {
            return List.of(tenantSchema.trim());
        }

        List<String> schemas = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT schema_name FROM information_schema.schemata " +
                             "WHERE schema_name LIKE 'tenant\\_%' ESCAPE '\\' ORDER BY schema_name"
             )) {
            while (resultSet.next()) {
                schemas.add(resultSet.getString(1));
            }
        }
        return schemas;
    }

    private void runLiquibase(String schemaName, String contexts) throws LiquibaseException {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(CHANGELOG_PATH);
        liquibase.setContexts(contexts);
        liquibase.setDefaultSchema(schemaName);
        liquibase.setLiquibaseSchema(schemaName);
        liquibase.afterPropertiesSet();
    }
}
