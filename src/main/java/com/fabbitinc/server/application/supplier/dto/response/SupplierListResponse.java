package com.fabbitinc.server.application.supplier.dto.response;

import java.util.List;

public record SupplierListResponse(
        long total,
        int offset,
        int limit,
        List<SupplierSummaryResponse> items
) {
}
