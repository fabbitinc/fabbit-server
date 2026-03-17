package com.fabbitinc.server.application.part.query.result;

import java.util.List;
import java.util.UUID;

public record PartLookupResult(
        List<Item> items
) {
    public record Item(
            UUID id,
            UUID revisionId,
            String partNumber,
            String name
    ) {
    }
}
