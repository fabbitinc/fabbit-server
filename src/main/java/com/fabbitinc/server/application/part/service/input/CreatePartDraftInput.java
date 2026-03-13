package com.fabbitinc.server.application.part.service.input;

public record CreatePartDraftInput(
        String partNumber,
        String baseRevisionCode
) {
}
