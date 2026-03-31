package com.fabbitinc.server.application.migration.usecase.result;

import java.util.List;
import java.util.UUID;

public record StartInventorMigrationResult(
        UUID sessionId,
        String projectName,
        int totalFileCount,
        int importableFileCount,
        List<UploadTargetResult> uploadTargets
) {
    public record UploadTargetResult(
            String path,
            UUID fileId,
            String uploadUrl,
            String fileKey
    ) {
    }
}
