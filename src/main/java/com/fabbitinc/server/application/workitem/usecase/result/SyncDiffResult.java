package com.fabbitinc.server.application.workitem.usecase.result;

public record SyncDiffResult(
        int addedCount,
        int removedCount
) {
}
