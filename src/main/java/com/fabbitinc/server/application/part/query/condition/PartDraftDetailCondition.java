package com.fabbitinc.server.application.part.query.condition;

public record PartDraftDetailCondition(
        String partNumber,
        String baseRevisionCode,
        String draftKey
) {
}
