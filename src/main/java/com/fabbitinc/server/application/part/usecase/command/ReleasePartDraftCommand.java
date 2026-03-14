package com.fabbitinc.server.application.part.usecase.command;

public record ReleasePartDraftCommand(
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String reason
) {
}
