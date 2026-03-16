package com.fabbitinc.server.application.part.query.condition;

public record PartFilesCondition(
        String partNumber,
        String revisionCode,
        String baseRevisionCode,
        String draftKey
) {
}
