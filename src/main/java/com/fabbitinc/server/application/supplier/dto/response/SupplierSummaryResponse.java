package com.fabbitinc.server.application.supplier.dto.response;

import java.util.UUID;

public record SupplierSummaryResponse(
        UUID id,
        String companyName,
        String code,
        String country
) {
}
