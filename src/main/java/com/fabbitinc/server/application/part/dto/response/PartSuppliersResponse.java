package com.fabbitinc.server.application.part.dto.response;

import java.util.List;

public record PartSuppliersResponse(
        long total,
        List<RelatedSupplierResponse> items
) {
}
