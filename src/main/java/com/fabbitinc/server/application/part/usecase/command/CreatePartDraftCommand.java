package com.fabbitinc.server.application.part.usecase.command;

public record CreatePartDraftCommand(
        String partNumber,
        String revisionCode,
        String reason
) {
}
