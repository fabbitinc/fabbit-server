package com.fabbitinc.server.infrastructure.persistence.tenant;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.application.organization.port.TenantProvisioningPort;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioningAdapter implements TenantProvisioningPort {

    private static final Pattern AGE_LABEL_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private static final List<DefaultLabel> DEFAULT_LABELS = List.of(
            new DefaultLabel("우선순위:높음", "즉시 처리 필요", "#b60205"),
            new DefaultLabel("우선순위:중간", "일반 처리", "#fbca04"),
            new DefaultLabel("우선순위:낮음", "여유 시 처리", "#0e8a16"),
            new DefaultLabel("설계변경", "설계 도면 또는 사양 변경", "#0075ca"),
            new DefaultLabel("품질", "품질 불량 및 결함 보고", "#d73a4a"),
            new DefaultLabel("개선", "기존 부품·공정 개선", "#a2eeef"),
            new DefaultLabel("원가절감", "원가 절감 활동", "#c5def5"),
            new DefaultLabel("공급사", "공급사 관련 문제", "#f9d0c4"),
            new DefaultLabel("시험검증", "시험·검증 요청", "#bfd4f2")
    );

    private final DataSource dataSource;

    @Override
    public String provisionTenant(UUID orgId) {
        String schemaName = TenantSchemaPolicy.schemaNameForOrgId(orgId);

        try {
            log.info("테넌트 프로비저닝 시작: orgId={}, schema={}", orgId, schemaName);

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true);
                loadAge(connection);
                setSearchPath(connection, "ag_catalog, public");
                ensureGraph(connection, schemaName);
            }
            log.info("AGE 그래프 준비 완료: schema={}", schemaName);

            applyTenantMigrations(schemaName);
            log.info("테넌트 마이그레이션 완료: schema={}", schemaName);

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(true);
                loadAge(connection);
                setSearchPath(connection, schemaName + ", ag_catalog, public");
                seedDefaultLabels(connection, schemaName);
                createOntologyIndexes(connection, schemaName);
            }
            log.info("기본 라벨/온톨로지 인덱스 완료: schema={}", schemaName);
        } catch (Exception ex) {
            log.error("테넌트 프로비저닝 실패: orgId={}, schema={}", orgId, schemaName, ex);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "테넌트 프로비저닝에 실패했습니다");
        }

        log.info("테넌트 프로비저닝 완료: orgId={}, schema={}", orgId, schemaName);
        return schemaName;
    }

    private void applyTenantMigrations(String schemaName) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            setSearchPath(connection, schemaName + ", public");

            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schemaName);
            database.setLiquibaseSchemaName(schemaName);

            try (var liquibase = new Liquibase(
                    "migrations/tenant-changelog.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            )) {
                liquibase.update();
            }
        }
    }

    private void setSearchPath(Connection connection, String searchPath) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('search_path', ?, false)"
        )) {
            statement.setString(1, searchPath);
            statement.execute();
        }
    }

    private void loadAge(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("LOAD 'age'");
        }
    }

    private void ensureGraph(Connection connection, String schemaName) throws SQLException {
        if (isGraphPresent(connection, schemaName)) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("SELECT ag_catalog.create_graph(?)")) {
            statement.setString(1, schemaName);
            statement.execute();
        }
    }

    private boolean isGraphPresent(Connection connection, String schemaName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM ag_catalog.ag_graph WHERE name = ?"
        )) {
            statement.setString(1, schemaName);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getLong(1) > 0;
            }
        }
    }

    private void seedDefaultLabels(Connection connection, String schemaName) throws SQLException {
        String sql = "INSERT INTO " + TenantSchemaPolicy.quoteIdentifier(schemaName) + ".labels "
                + "(id, name, description, color, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, now(), now()) ON CONFLICT (name) DO NOTHING";

        for (DefaultLabel defaultLabel : DEFAULT_LABELS) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, UuidV7Generator.next());
                statement.setString(2, defaultLabel.name());
                statement.setString(3, defaultLabel.description());
                statement.setString(4, defaultLabel.color());
                statement.executeUpdate();
            }
        }
    }

    private void createOntologyIndexes(Connection connection, String graphName) throws SQLException {
        Set<String> createdLabels = new LinkedHashSet<>();

        for (ManufacturingOntology.NodeLabelDef nodeLabel : ManufacturingOntology.ONTOLOGY.nodeLabels()) {
            String label = nodeLabel.label();
            validateGraphLabel(label);

            if (!createdLabels.contains(label) && !isGraphLabelPresent(connection, graphName, label)) {
                String sql = "SELECT ag_catalog.create_vlabel('"
                        + TenantSchemaPolicy.normalizeSqlIdentifier(graphName) + "', '"
                        + label + "')";
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
                createdLabels.add(label);
            }

            for (ManufacturingOntology.PropertyDef property : nodeLabel.properties()) {
                if (!property.isIndexed()) {
                    continue;
                }
                String propertyName = TenantSchemaPolicy.normalizeSqlIdentifier(property.name());
                String indexName = TenantSchemaPolicy.normalizeSqlIdentifier(
                        "ix_" + label.toLowerCase(Locale.ROOT) + "_" + propertyName
                );
                createAgtypePropertyIndex(connection, graphName, label, propertyName, indexName);
            }
        }
    }

    private boolean isGraphLabelPresent(Connection connection, String graphName, String labelName) throws SQLException {
        String sql = """
                SELECT count(*)
                FROM ag_catalog.ag_label l
                JOIN ag_catalog.ag_graph g ON g.graphid = l.graph
                WHERE g.name = ? AND l.name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, graphName);
            statement.setString(2, labelName);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getLong(1) > 0;
            }
        }
    }

    private void createAgtypePropertyIndex(
            Connection connection,
            String schemaName,
            String labelName,
            String propertyName,
            String indexName
    ) throws SQLException {
        String quotedSchema = TenantSchemaPolicy.quoteIdentifier(schemaName);
        String quotedLabel = quoteGraphLabel(labelName);
        String quotedIndex = TenantSchemaPolicy.quoteIdentifier(indexName);
        String escapedPropertyName = propertyName.replace("'", "''");

        String sql = "CREATE INDEX IF NOT EXISTS " + quotedIndex
                + " ON " + quotedSchema + "." + quotedLabel
                + " USING btree ((ag_catalog.agtype_access_operator(properties, '\""
                + escapedPropertyName + "\"'::agtype)))";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void validateGraphLabel(String labelName) {
        if (!AGE_LABEL_PATTERN.matcher(labelName).matches()) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "온톨로지 라벨명이 유효하지 않습니다: " + labelName);
        }
    }

    private String quoteGraphLabel(String labelName) {
        validateGraphLabel(labelName);
        return "\"" + labelName + "\"";
    }

    private record DefaultLabel(String name, String description, String color) {
    }
}
