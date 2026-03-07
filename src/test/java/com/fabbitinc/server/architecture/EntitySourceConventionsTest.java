package com.fabbitinc.server.architecture;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class EntitySourceConventionsTest {

    private static final Path DOMAIN_ROOT = Path.of("src/main/java/com/fabbitinc/server/domain");
    private static final Pattern ENTITY_OR_MAPPED_SUPERCLASS_PATTERN = Pattern.compile(
            "(?m)^\\s*@(Entity|MappedSuperclass)\\b"
    );
    private static final Pattern FORBIDDEN_LOMBOK_PATTERN = Pattern.compile(
            "(?m)^\\s*@(Setter|Data|EqualsAndHashCode)\\b"
    );

    @Test
    void entitiesMustNotUseForbiddenLombokAnnotations() throws IOException {
        List<String> violations;
        try (Stream<Path> paths = Files.walk(DOMAIN_ROOT)) {
            violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .map(this::readFile)
                    .filter(file -> ENTITY_OR_MAPPED_SUPERCLASS_PATTERN.matcher(file.content()).find())
                    .filter(file -> FORBIDDEN_LOMBOK_PATTERN.matcher(file.content()).find())
                    .map(file -> file.path().toString())
                    .sorted()
                    .toList();
        }

        if (!violations.isEmpty()) {
            fail("금지된 Lombok 어노테이션을 사용한 엔티티/공통 엔티티가 있습니다: " + String.join(", ", violations));
        }
    }

    private SourceFile readFile(Path path) {
        try {
            return new SourceFile(path, Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("소스 파일을 읽을 수 없습니다: " + path, exception);
        }
    }

    private record SourceFile(Path path, String content) {
    }
}
