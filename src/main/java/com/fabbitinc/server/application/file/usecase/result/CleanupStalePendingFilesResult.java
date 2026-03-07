package com.fabbitinc.server.application.file.usecase.result;

public record CleanupStalePendingFilesResult(
        int deletedCount
) {
}
