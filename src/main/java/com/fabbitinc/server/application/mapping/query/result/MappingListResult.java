package com.fabbitinc.server.application.mapping.query.result;

import java.util.List;

public record MappingListResult(
        List<MappingResult> items
) {
    public MappingListResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
