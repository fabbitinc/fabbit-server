package com.fabbitinc.server.application.subscription.usecase.result;

import java.time.Instant;

public record RecordStorageUsageSnapshotsResult(
        int snapshotCount,
        Instant snapshotAt
) {
}
