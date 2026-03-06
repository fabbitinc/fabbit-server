package com.fabbitinc.server.application.label.query.result;

import java.util.List;

public record LabelListResult(
        int total,
        List<LabelResult> items
) {
    public LabelListResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
