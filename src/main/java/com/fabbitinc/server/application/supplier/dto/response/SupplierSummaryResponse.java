package com.fabbitinc.server.application.supplier.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "공급사 요약 정보")
public record SupplierSummaryResponse(
        @Schema(description = "공급사 ID")
        UUID id,
        @Schema(description = "회사명", example = "Samsung Electro-Mechanics")
        String companyName,
        @Schema(description = "공급사 코드", example = "SEM")
        String code,
        @Schema(description = "국가", example = "KR")
        String country
) {
}
