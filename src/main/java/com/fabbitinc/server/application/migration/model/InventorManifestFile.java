package com.fabbitinc.server.application.migration.model;

public record InventorManifestFile(
        String path,
        String originalName,
        InventorManifestFileType type,
        String contentType,
        long sizeBytes,
        String contentHash
) {
}
