package com.fabbitinc.server.application.mappingv2.query.result;

import java.util.List;

public record MappingV2ListResult(
        List<MappingV2Result> items
) {
    public MappingV2ListResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
