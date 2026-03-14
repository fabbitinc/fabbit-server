package com.fabbitinc.server.application.part.service.input;

public record ReleasePartRevisionInput(
        String partNumber,
        String revisionCode,
        String reason
) {
}
