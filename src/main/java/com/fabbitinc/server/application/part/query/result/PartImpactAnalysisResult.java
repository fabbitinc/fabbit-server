package com.fabbitinc.server.application.part.query.result;

import java.util.List;
import java.util.UUID;

public record PartImpactAnalysisResult(
        List<AffectedBomItem> bomItems,
        List<AffectedProject> projects,
        Summary summary
) {
    public record AffectedBomItem(
            UUID parentPartId,
            String parentPartNumber,
            String parentPartName,
            String parentRevisionCode,
            int level
    ) {
    }

    public record AffectedProject(
            UUID projectId,
            String projectName
    ) {
    }

    public record Summary(
            int affectedBomCount,
            int affectedProjectCount,
            int draftRevisionCount,
            List<UUID> suggestedReviewerIds,
            boolean truncated,
            int totalCount
    ) {
    }
}
