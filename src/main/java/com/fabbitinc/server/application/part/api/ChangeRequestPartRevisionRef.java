package com.fabbitinc.server.application.part.api;

public record ChangeRequestPartRevisionRef(
        String partNumber,
        String baseRevisionCode,
        String draftKey
) {
}
