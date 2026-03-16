package com.fabbitinc.server.application.part.query.condition;

public record PartPreviewSourcesCondition(
        String partNumber,
        String revisionCode,
        String baseRevisionCode,
        String draftKey
) {
}
