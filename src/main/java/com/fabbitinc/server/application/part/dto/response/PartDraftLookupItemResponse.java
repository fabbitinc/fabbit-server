package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부품 초안 lookup 항목")
public record PartDraftLookupItemResponse(
        @Schema(description = "리비전 ID")
        UUID revisionId,
        @Schema(description = "부품 ID")
        UUID partId,
        @Schema(description = "품번", example = "AES-100")
        String partNumber,
        @Schema(description = "기준 공식 리비전 코드", example = "1")
        String baseRevisionCode,
        @Schema(description = "초안 키", example = "D1")
        String draftKey,
        @Schema(description = "초안 이름", example = "메인 하우징")
        String name,
        @Schema(description = "초안 작성자")
        PartOwnerUserSummaryResponse createdBy
) {
}
