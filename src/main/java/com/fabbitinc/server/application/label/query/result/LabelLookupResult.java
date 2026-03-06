package com.fabbitinc.server.application.label.query.result;

import java.util.List;

public record LabelLookupResult(
        List<LabelLookupItemResult> items
) {
    public LabelLookupResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
