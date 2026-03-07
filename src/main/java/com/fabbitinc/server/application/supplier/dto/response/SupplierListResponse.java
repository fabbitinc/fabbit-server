package com.fabbitinc.server.application.supplier.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "공급사 목록 응답")
public record SupplierListResponse(
        @Schema(description = "전체 건수", example = "125")
        long total,
        @Schema(description = "현재 오프셋", example = "0")
        int offset,
        @Schema(description = "조회 건수", example = "20")
        int limit,
        @Schema(description = "공급사 목록")
        List<SupplierSummaryResponse> items
) {
}
