package com.fabbitinc.server.tools;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
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

        String target = args.length > 0 ? args[0] : "all";

        File tmpFile = File.createTempFile("schema-export-", ".sql");
        tmpFile.deleteOnExit();

        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
        settings.put("jakarta.persistence.schema-generation.scripts.create-target", tmpFile.getAbsolutePath());

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        MetadataSources sources = new MetadataSources(registry);

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        for (var bd : scanner.findCandidateComponents("com.fabbitinc.server.domain")) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());
            if (matchesTarget(clazz, target)) {
                sources.addAnnotatedClass(clazz);
            }
        }

        Metadata metadata = sources.buildMetadata();

        SchemaManagementToolCoordinator.process(
                metadata,
                registry,
                settings,
                action -> {}
        );

        System.out.print(Files.readString(tmpFile.toPath()));
    }

    private static boolean matchesTarget(Class<?> clazz, String target) {
        if ("all".equals(target)) {
            return true;
        }
        Table table = clazz.getAnnotation(Table.class);
        boolean isPublic = table != null && "public".equals(table.schema());
        return "public".equals(target) ? isPublic : !isPublic;
    }
}
