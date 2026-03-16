package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "리비전 상세 diff 응답")
public record PartRevisionDiffResponse(
        PartRevisionDiffRevisionResponse baseRevision,
        PartRevisionDiffRevisionResponse targetRevision,
        PartRevisionDiffSummaryResponse summary,
        List<PartRevisionDiffAttributeChangeResponse> attributes,
        List<PartRevisionDiffFileChangeResponse> files,
        List<PartRevisionDiffBomChangeResponse> bom,
        List<PartRevisionDiffAssigneeChangeResponse> assignees
) {
}
