package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record PartSuppliersResponse(
        long total,
        List<RelatedSupplierResponse> items
) {
}
