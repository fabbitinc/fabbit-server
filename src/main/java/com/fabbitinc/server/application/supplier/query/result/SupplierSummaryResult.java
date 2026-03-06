package com.fabbitinc.server.application.supplier.query.result;

import java.util.UUID;

public record SupplierSummaryResult(
        UUID id,
        String companyName,
        String code,
        String country
) {
}
