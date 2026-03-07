package com.fabbitinc.server.application.file.service.output;

public record CleanupOrphanObjectsOutput(
        int deletedCount,
        int scannedPageCount,
        int scannedObjectCount,
        boolean stoppedByPageLimit,
        boolean stoppedByDeleteLimit
) {
}
