package com.fabbitinc.server.application.part.usecase.command;

import java.util.List;
import java.util.UUID;

public record AttachPartFilesCommand(
        String partNumber,
        String revisionCode,
        String baseRevisionCode,
        String draftKey,
        List<UUID> fileIds
) {
}
