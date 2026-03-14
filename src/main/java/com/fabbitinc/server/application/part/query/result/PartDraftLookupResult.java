package com.fabbitinc.server.application.part.query.result;

import java.util.List;
import java.util.UUID;

public record PartDraftLookupResult(
        List<Item> items
) {
    public record Item(
            UUID revisionId,
            UUID partId,
            String partNumber,
            String baseRevisionCode,
            String draftKey,
            String name,
            PartUserSummaryResult createdBy
    ) {
    }
}
