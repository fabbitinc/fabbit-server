package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "부품 초안 lookup 응답")
public record PartDraftLookupResponse(
        @Schema(description = "초안 목록")
        List<PartDraftLookupItemResponse> items
) {
}
