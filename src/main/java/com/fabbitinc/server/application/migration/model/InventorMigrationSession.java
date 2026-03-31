package com.fabbitinc.server.application.migration.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InventorMigrationSession(
        UUID sessionId,
        UUID orgId,
        UUID userId,
        String projectName,
        String ipjPath,
        String inventorVersion,
        Instant createdAt,
        Instant expiresAt,
        List<InventorManifestFile> files,
        Map<String, UUID> manifestPathToFileId
) {
    public InventorMigrationSession {
        files = files == null ? List.of() : List.copyOf(files);
        manifestPathToFileId = manifestPathToFileId == null ? Map.of() : new LinkedHashMap<>(manifestPathToFileId);
    }

    public List<UUID> fileIds() {
        return List.copyOf(manifestPathToFileId.values());
    }

    public UUID fileIdOf(String path) {
        return manifestPathToFileId.get(path);
    }

    public String ipjFileName() {
        if (ipjPath == null || ipjPath.isBlank()) {
            return null;
        }
        int lastSlash = Math.max(ipjPath.lastIndexOf('/'), ipjPath.lastIndexOf('\\'));
        return lastSlash >= 0 ? ipjPath.substring(lastSlash + 1) : ipjPath;
    }
}
