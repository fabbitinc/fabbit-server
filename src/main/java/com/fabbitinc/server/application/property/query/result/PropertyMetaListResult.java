package com.fabbitinc.server.application.property.query.result;

import java.util.List;

public record PropertyMetaListResult(
        int total,
        List<PropertyMetaResult> items
) {
}
