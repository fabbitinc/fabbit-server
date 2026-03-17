package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.util.List;
import java.util.UUID;

public record PartRevisionLookupResult(
        List<Item> items
) {
    public record Item(
            UUID revisionId,
            UUID partId,
            String partNumber,
            String baseRevisionCode,
            String name,
            PartRevisionStatus status,
            PartUserSummaryResult createdBy
    ) {
    }
}
