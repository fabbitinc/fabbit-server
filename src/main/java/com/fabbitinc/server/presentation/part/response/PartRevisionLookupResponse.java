package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "부품 리비전 lookup 응답")
public record PartRevisionLookupResponse(
        @Schema(description = "리비전 목록")
        List<PartRevisionLookupItemResponse> items
) {
}
