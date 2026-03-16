package com.fabbitinc.server.application.workitem.usecase.result;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;

public record SyncDiffResult(
        int addedCount,
        int removedCount
) {
}
