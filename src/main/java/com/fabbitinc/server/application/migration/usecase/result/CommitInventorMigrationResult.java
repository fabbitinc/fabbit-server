package com.fabbitinc.server.application.migration.usecase.result;

import java.util.List;
import java.util.UUID;

public record CommitInventorMigrationResult(
        UUID projectId,
        List<UUID> createdPartIds,
        Summary summary
) {
    public record Summary(
            int createdPartCount,
            int orphanDrawingCount
    ) {
    }
}
