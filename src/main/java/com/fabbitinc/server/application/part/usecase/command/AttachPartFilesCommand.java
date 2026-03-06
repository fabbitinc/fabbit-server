package com.fabbitinc.server.application.part.usecase.command;

import java.util.List;
import java.util.UUID;

public record AttachPartFilesCommand(
        UUID partId,
        List<UUID> fileIds
) {
}
