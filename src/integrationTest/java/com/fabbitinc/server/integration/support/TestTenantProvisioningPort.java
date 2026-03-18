package com.fabbitinc.server.integration.support;

import com.fabbitinc.server.application.organization.port.TenantProvisioningPort;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class TestTenantProvisioningPort implements TenantProvisioningPort {

    private final DataSource dataSource;

    public TestTenantProvisioningPort(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String provisionTenant(UUID orgId) {
        String schemaName = TenantSchemaPolicy.schemaNameForOrgId(orgId);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + TenantSchemaPolicy.quoteIdentifier(schemaName));
                statement.execute("SET search_path TO " + TenantSchemaPolicy.quoteIdentifier(schemaName) + ", public");
            }

            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schemaName);
            database.setLiquibaseSchemaName(schemaName);

            try (Liquibase liquibase = new Liquibase(
                    "migrations/changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            )) {
                liquibase.update(new Contexts("tenant"), new LabelExpression());
            }
            return schemaName;
        } catch (Exception ex) {
            throw new IllegalStateException("테스트 테넌트 프로비저닝에 실패했습니다", ex);
        }
    }
}
