package com.fabbitinc.server.infrastructure.persistence.tenant;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.application.organization.port.TenantProvisioningPort;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Override
    public String provisionTenant(UUID orgId) {
        String schemaName = TenantSchemaPolicy.schemaNameForOrgId(orgId);

        try {
            log.info("테넌트 프로비저닝 시작: orgId={}, schema={}", orgId, schemaName);

            try (Connection connection = dataSource.getConnection()) {
                loadAge(connection);
                ensureGraph(connection, schemaName);
            }
            log.info("AGE 그래프 준비 완료: schema={}", schemaName);

            applyTenantMigrations(schemaName);
            log.info("테넌트 마이그레이션 완료: schema={}", schemaName);

            try (Connection connection = dataSource.getConnection()) {
                loadAge(connection);
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

    private void applyTenantMigrations(String schemaName) throws IOException, InterruptedException {
        Path tmpDir = extractMigrationFiles();
        try {
            String atlasUrl = toAtlasUrl(schemaName);
            ProcessBuilder pb = new ProcessBuilder(
                    "atlas", "migrate", "apply",
                    "--dir", "file://" + tmpDir.toAbsolutePath(),
                    "--url", atlasUrl,
                    "--lock-timeout", "3s"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.getOutputStream().close();

            List<String> lines = new ArrayList<>();
            Thread outputReader = Thread.ofVirtual().start(() -> readProcessOutput(process.getInputStream(), lines));

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                outputReader.join(2_000);
                String output = String.join("\n", lines);
                log.error("atlas migrate apply 타임아웃: schema={}, output={}", schemaName, output);
                throw new IOException("atlas migrate apply timed out");
            }

            outputReader.join(2_000);
            String output = String.join("\n", lines);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("atlas migrate apply 실패: schema={}, output={}", schemaName, output);
                throw new IOException("atlas migrate apply failed with exit code " + exitCode);
            }
            log.info("atlas migrate apply 완료: schema={}", schemaName);
        } finally {
            deleteDirectory(tmpDir);
        }
    }

    private Path extractMigrationFiles() throws IOException {
        Path tmpDir = Files.createTempDirectory("tenant-migrations-");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:tenant/*");
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, tmpDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return tmpDir;
    }

    private String toAtlasUrl(String schemaName) {
        String pgUrl = jdbcUrl
                .replace("jdbc:postgresql://", "postgres://" + dbUsername + ":" + dbPassword + "@");
        String separator = pgUrl.contains("?") ? "&" : "?";
        return pgUrl + separator + "search_path=" + schemaName + "&sslmode=disable";
    }

    private void deleteDirectory(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private void readProcessOutput(InputStream inputStream, List<String> lines) {
        try (inputStream) {
            String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (!output.isBlank()) {
                lines.add(output);
            }
        } catch (IOException ignored) {
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
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT ag_catalog.create_vlabel(?, ?)"
                )) {
                    statement.setString(1, graphName);
                    statement.setString(2, label);
                    statement.execute();
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
