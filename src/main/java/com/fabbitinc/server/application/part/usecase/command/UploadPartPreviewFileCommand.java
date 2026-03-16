package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record UploadPartPreviewFileCommand(
        String partNumber,
        String revisionCode,
        UUID fileId
) {
}
