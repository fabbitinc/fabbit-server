package com.fabbitinc.server.application.bom.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.util.List;
import java.util.UUID;

public record WhereUsedSummaryResult(
        int directReferenceCount,
        StatusBreakdown statusBreakdown,
        List<Reference> references
) {

    public record StatusBreakdown(
            int draftCount,
            int releasedCount,
            int supersededCount,
            int canceledCount
    ) {
    }

    public record Reference(
            UUID partId,
            String partNumber,
            String partName,
            UUID revisionId,
            String revisionCode,
            PartRevisionStatus revisionStatus
    ) {
    }
}
