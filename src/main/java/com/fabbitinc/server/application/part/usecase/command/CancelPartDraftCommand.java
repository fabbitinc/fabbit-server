package com.fabbitinc.server.application.part.usecase.command;

public record CancelPartDraftCommand(
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String reason
) {
}
