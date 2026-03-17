package com.fabbitinc.server.application.part.usecase.command;

import java.util.List;
import java.util.UUID;

public record AttachPartFilesCommand(
        UUID partId,
        UUID revisionId,
        List<UUID> fileIds
) {
}
