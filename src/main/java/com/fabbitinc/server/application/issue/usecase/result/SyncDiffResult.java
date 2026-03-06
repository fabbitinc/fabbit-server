package com.fabbitinc.server.application.issue.usecase.result;

public record SyncDiffResult(
        int addedCount,
        int removedCount
) {
}
