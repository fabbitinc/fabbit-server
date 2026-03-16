package com.fabbitinc.server.application.part.api;

public record EngineeringChangePartRevisionRef(
        String partNumber,
        String baseRevisionCode,
        String draftKey
) {
}
