package com.fabbitinc.server.application.file.usecase.result;

public record CleanupExpiredDeletedFilesResult(
        int deletedCount
) {
}
