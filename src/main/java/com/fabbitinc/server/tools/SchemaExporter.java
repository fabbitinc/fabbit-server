package com.fabbitinc.server.tools;

import jakarta.persistence.Entity;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class SchemaExporter {

    public static void main(String[] args) throws Exception {
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(
                org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.OFF);

        System.out.print(exportSql(args.length > 0 ? args[0] : "all"));
    }

    static String exportSql(String targetName) throws Exception {
        ExportTarget target = ExportTarget.from(targetName);

        File tmpFile = File.createTempFile("schema-export-", ".sql");
        tmpFile.deleteOnExit();

        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
        settings.put("jakarta.persistence.schema-generation.scripts.create-target", tmpFile.getAbsolutePath());
        settings.put(SchemaToolingSettings.HBM2DDL_FILTER_PROVIDER, new ExportSchemaFilterProvider(target));

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        try {
            MetadataSources sources = new MetadataSources(registry);

            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

            for (var bd : scanner.findCandidateComponents("com.fabbitinc.server.domain")) {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                sources.addAnnotatedClass(clazz);
            }

            Metadata metadata = sources.buildMetadata();

            SchemaManagementToolCoordinator.process(
                    metadata,
                    registry,
                    settings,
                    action -> {}
            );

            return Files.readString(tmpFile.toPath());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private enum ExportTarget {
        ALL,
        PUBLIC,
        TENANT;

        static ExportTarget from(String raw) {
            return switch (raw) {
                case "all" -> ALL;
                case "public" -> PUBLIC;
                case "tenant" -> TENANT;
                default -> throw new IllegalArgumentException("지원하지 않는 schema export 대상입니다: " + raw);
            };
        }

        boolean includesSchemaName(String schemaName) {
            boolean isPublic = "public".equals(schemaName);
            return switch (this) {
                case ALL -> true;
                case PUBLIC -> isPublic;
                case TENANT -> !isPublic;
            };
        }
    }

    private record ExportSchemaFilterProvider(ExportTarget target) implements SchemaFilterProvider {

        @Override
        public SchemaFilter getCreateFilter() {
            return new ExportSchemaFilter(target);
        }

        @Override
        public SchemaFilter getDropFilter() {
            return getCreateFilter();
        }

        @Override
        public SchemaFilter getTruncatorFilter() {
            return getCreateFilter();
        }

        @Override
        public SchemaFilter getMigrateFilter() {
            return getCreateFilter();
        }

        @Override
        public SchemaFilter getValidateFilter() {
            return getCreateFilter();
        }
    }

    private record ExportSchemaFilter(ExportTarget target) implements SchemaFilter {

        @Override
        public boolean includeNamespace(Namespace namespace) {
            var schemaIdentifier = namespace.getPhysicalName().schema();
            return target.includesSchemaName(schemaIdentifier == null ? null : schemaIdentifier.getText());
        }

        @Override
        public boolean includeTable(Table table) {
            return target.includesSchemaName(table.getSchema());
        }

        @Override
        public boolean includeSequence(Sequence sequence) {
            var schemaIdentifier = sequence.getName().getSchemaName();
            return target.includesSchemaName(schemaIdentifier == null ? null : schemaIdentifier.getText());
        }
    }
}
