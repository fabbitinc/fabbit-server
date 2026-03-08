package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawingTempDirectoryCleanupService {

    private final DrawingConverterProperties drawingConverterProperties;

    public int cleanupExpiredWorkDirectories() {
        Path rootPath = Path.of(drawingConverterProperties.tempDir());
        if (!Files.isDirectory(rootPath)) {
            return 0;
        }

        Instant cutoff = Instant.now().minus(Duration.ofHours(drawingConverterProperties.tempDirCleanupMaxAgeHours()));
        int deletedCount = 0;

        try (Stream<Path> children = Files.list(rootPath)) {
            for (Path childPath : children.toList()) {
                if (!isExpiredDrawingWorkDirectory(childPath, cutoff)) {
                    continue;
                }
                try {
                    deleteRecursively(childPath);
                    deletedCount++;
                } catch (IOException ex) {
                    log.warn(
                            "event=drawing_temp_dir_cleanup_delete_failed path={} reason={}",
                            childPath,
                            ex.getMessage(),
                            ex
                    );
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("도면 작업 임시 디렉터리 정리에 실패했습니다", ex);
        }
        return deletedCount;
    }

    private boolean isExpiredDrawingWorkDirectory(Path path, Instant cutoff) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        if (!fileName.startsWith("drawing-")) {
            return false;
        }
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException ex) {
            log.warn(
                    "event=drawing_temp_dir_cleanup_metadata_failed path={} reason={}",
                    path,
                    ex.getMessage(),
                    ex
            );
            return false;
        }
    }

    private void deleteRecursively(Path rootPath) throws IOException {
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("도면 작업 임시 디렉터리를 삭제할 수 없습니다: " + path, ex);
                        }
                    });
        }
    }
}
