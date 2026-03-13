package com.fabbitinc.server.application.part.usecase.command;

public record ClearPartPreviewCommand(
        String partNumber,
        String revisionCode
) {
}
