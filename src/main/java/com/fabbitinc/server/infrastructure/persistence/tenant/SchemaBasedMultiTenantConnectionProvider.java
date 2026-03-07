package com.fabbitinc.server.infrastructure.persistence.tenant;

import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

@Component
public class SchemaBasedMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final String SEARCH_PATH_SQL = "SELECT set_config('search_path', ?, false)";
    private static final String DEFAULT_SEARCH_PATH = "ag_catalog, public";

    private final DataSource dataSource;

    public SchemaBasedMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        setSearchPath(connection, TenantSchemaPolicy.PUBLIC_SCHEMA);
        return connection;
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        resetSearchPath(connection);
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        String schemaName = TenantSchemaPolicy.normalizeSchemaName(tenantIdentifier);
        Connection connection = dataSource.getConnection();
        setSearchPath(connection, schemaName);
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        resetSearchPath(connection);
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new IllegalArgumentException("지원하지 않는 unwrap 타입입니다: " + unwrapType.getName());
    }

    @Override
    public boolean handlesConnectionSchema() {
        return true;
    }

    private void setSearchPath(Connection connection, String schemaName) throws SQLException {
        String normalizedSchema = TenantSchemaPolicy.normalizeSchemaName(schemaName);
        String searchPath = TenantSchemaPolicy.PUBLIC_SCHEMA.equals(normalizedSchema)
                ? DEFAULT_SEARCH_PATH
                : normalizedSchema + ", " + DEFAULT_SEARCH_PATH;
        applySearchPath(connection, searchPath);
    }

    private void resetSearchPath(Connection connection) throws SQLException {
        applySearchPath(connection, DEFAULT_SEARCH_PATH);
    }

    private void applySearchPath(Connection connection, String searchPath) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SEARCH_PATH_SQL)) {
            statement.setString(1, searchPath);
            statement.execute();
        }
    }
}
