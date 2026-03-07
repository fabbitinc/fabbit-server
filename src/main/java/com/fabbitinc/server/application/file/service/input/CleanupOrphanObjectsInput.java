package com.fabbitinc.server.application.file.service.input;

import java.time.Duration;
import java.util.UUID;

public record CleanupOrphanObjectsInput(
        UUID orgId,
        int listBatchSize,
        int maxListPages,
        int maxDeleteCount,
        Duration pauseBetweenPages
) {
}
