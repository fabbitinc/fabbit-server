package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record UploadPartPreviewFileCommand(
        UUID partId,
        UUID revisionId,
        UUID fileId
) {
}
