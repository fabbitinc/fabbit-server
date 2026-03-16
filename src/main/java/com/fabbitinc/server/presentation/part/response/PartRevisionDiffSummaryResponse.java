package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리비전 diff 요약")
public record PartRevisionDiffSummaryResponse(
        long attributeChanges,
        long fileChanges,
        long bomChanges
) {
}
