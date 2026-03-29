package com.fabbitinc.server.application.issue.query.result;

import java.util.List;
import java.util.UUID;

public record EcPrefillResult(
        String suggestedTitle,
        List<AffectedItemSuggestion> affectedItems,
        List<UUID> suggestedReviewerIds,
        ImpactSummary impactSummary
) {
    public record AffectedItemSuggestion(
            UUID partId,
            String partNumber,
            UUID revisionId,
            String revisionCode
    ) {
    }

    public record ImpactSummary(
            int affectedBomCount,
            int affectedProjectCount,
            int draftRevisionCount
    ) {
    }
}
