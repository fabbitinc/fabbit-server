package com.fabbitinc.server.application.file.usecase.command;

import java.util.List;
import java.util.UUID;

public record BatchCompleteFilesCommand(
        List<UUID> fileIds
) {
}
