package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record DeletePartPreviewFileCommand(
        UUID partId,
        UUID revisionId,
        UUID previewFileId
) {
}
