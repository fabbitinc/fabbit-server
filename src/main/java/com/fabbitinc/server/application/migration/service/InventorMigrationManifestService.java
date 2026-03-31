package com.fabbitinc.server.application.migration.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorManifestFileType;
import com.fabbitinc.server.application.migration.support.InventorMigrationAnalyzer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InventorMigrationManifestService {

    private static final int MAX_IMPORTABLE_COUNT = 500;

    private final InventorMigrationAnalyzer inventorMigrationAnalyzer = new InventorMigrationAnalyzer();

    public InventorManifestFile normalizeFile(InventorManifestFile file) {
        return new InventorManifestFile(
                normalizePath(file.path()),
                normalizeOriginalName(file.originalName(), file.path()),
                requireType(file.type()),
                normalizeContentType(file.contentType()),
                requirePositive(file.sizeBytes(), "sizeBytes는 0보다 커야 합니다"),
                normalizeContentHash(file.contentHash())
        );
    }

    public void validateStartInput(String projectName, String ipjPath, List<InventorManifestFile> files) {
        requireText(projectName, "projectName은 필수입니다");
        requireText(ipjPath, "ipjPath는 필수입니다");
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "files는 1개 이상이어야 합니다");
        }

        Set<String> seenPaths = new LinkedHashSet<>();
        for (InventorManifestFile file : files) {
            if (!seenPaths.add(file.path())) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "중복된 path가 있습니다: " + file.path());
            }
        }

        int importableCount = countImportable(files);
        if (importableCount < 1) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "가져오기 대상 PART 또는 ASSEMBLY 파일이 1개 이상 필요합니다");
        }
        if (importableCount > MAX_IMPORTABLE_COUNT) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "가져오기 대상 파일은 최대 500개까지 가능합니다");
        }
    }

    public int countImportable(List<InventorManifestFile> files) {
        return inventorMigrationAnalyzer.countImportable(files);
    }

    public InventorMigrationAnalyzer.Analysis analyze(com.fabbitinc.server.application.migration.model.InventorMigrationSession session) {
        return inventorMigrationAnalyzer.analyze(session);
    }

    public String derivePartNumber(InventorManifestFile file) {
        return inventorMigrationAnalyzer.derivePartNumber(file.path());
    }

    private String normalizePath(String value) {
        String normalized = requireText(value, "path는 필수입니다").replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized;
    }

    private String normalizeOriginalName(String originalName, String path) {
        String normalized = originalName == null || originalName.isBlank() ? fallbackFileName(path) : originalName.trim();
        if (normalized.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "originalName은 필수입니다");
        }
        return normalized;
    }

    private String fallbackFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private InventorManifestFileType requireType(InventorManifestFileType type) {
        if (type == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "type은 필수입니다");
        }
        return type;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeContentType(String value) {
        if (value == null || value.isBlank()) {
            return "application/octet-stream";
        }
        return value.trim();
    }

    private String normalizeContentHash(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long requirePositive(long value, String message) {
        if (value <= 0L) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

}
