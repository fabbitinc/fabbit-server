package com.fabbitinc.server.application.supplier.query.result;

import java.util.List;

public record SupplierListResult(
        long total,
        int offset,
        int limit,
        List<SupplierSummaryResult> items
) {
    public SupplierListResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
