package com.fabbitinc.server.application.part.usecase.command;

public record ReleasePartRevisionCommand(
        String partNumber,
        String revisionCode,
        String reason
) {
}
