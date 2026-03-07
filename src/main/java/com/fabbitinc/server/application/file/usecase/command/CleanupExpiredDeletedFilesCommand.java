package com.fabbitinc.server.application.file.usecase.command;

import java.time.Duration;

public record CleanupExpiredDeletedFilesCommand(
        Duration retention,
        int batchSize
) {
}
