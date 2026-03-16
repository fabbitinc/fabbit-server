package com.fabbitinc.server.application.part.query.result;

public record PartRevisionDiffSummaryResult(
        long attributeChanges,
        long fileChanges,
        long bomChanges,
        long assigneeChanges
) {
}
