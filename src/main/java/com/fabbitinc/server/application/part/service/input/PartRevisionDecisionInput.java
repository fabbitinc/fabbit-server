package com.fabbitinc.server.application.part.service.input;

public record PartRevisionDecisionInput(
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String reason
) {
}
