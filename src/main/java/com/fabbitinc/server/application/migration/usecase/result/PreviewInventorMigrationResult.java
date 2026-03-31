package com.fabbitinc.server.application.migration.usecase.result;

import java.util.List;
import java.util.UUID;

public record PreviewInventorMigrationResult(
        UUID sessionId,
        String projectName,
        Summary summary,
        List<ItemResult> items,
        List<OrphanDrawingResult> orphanDrawings,
        boolean readyToCommit
) {
    public record Summary(
            int totalFileCount,
            int importableFileCount,
            int readyItemCount,
            int warningCount,
            int errorCount
    ) {
    }

    public record ItemResult(
            String path,
            String fileType,
            String derivedPartNumber,
            UUID modelFileId,
            boolean uploaded,
            String status,
            String message,
            List<UUID> drawingFileIds,
            List<String> drawingPaths
    ) {
    }

    public record OrphanDrawingResult(
            String path,
            UUID fileId,
            boolean uploaded,
            String message
    ) {
    }
}
