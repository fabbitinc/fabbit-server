package com.fabbitinc.server.application.part.usecase.command;

public record ApprovePartRevisionCommand(
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String reason
) {
}
