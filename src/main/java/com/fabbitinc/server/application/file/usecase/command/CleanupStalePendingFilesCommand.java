package com.fabbitinc.server.application.file.usecase.command;

import java.time.Duration;

public record CleanupStalePendingFilesCommand(
        Duration maxAge,
        int batchSize
) {
}
