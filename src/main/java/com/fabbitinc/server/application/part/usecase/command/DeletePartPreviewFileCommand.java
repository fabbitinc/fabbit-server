package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record DeletePartPreviewFileCommand(
        String partNumber,
        String revisionCode,
        String baseRevisionCode,
        String draftKey,
        UUID previewFileId
) {
}
