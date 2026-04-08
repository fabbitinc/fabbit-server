package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "부품 리비전 lookup 항목")
public record PartRevisionLookupItemResponse(
        @Schema(description = "리비전 ID")
        UUID revisionId,
        @Schema(description = "부품 ID")
        UUID partId,
        @Schema(description = "품번", example = "AES-100")
        String partNumber,
        @Schema(description = "리비전 코드", example = "2")
        String revisionCode,
        @Schema(description = "기준 공식 리비전 코드", example = "1")
        String baseRevisionCode,
        @Schema(description = "리비전 이름", example = "메인 하우징")
        String name,
        @Schema(description = "리비전 상태", example = "DRAFT")
        PartRevisionStatus status,
        @Schema(description = "리비전 생성 시각")
        Instant createdAt,
        @Schema(description = "현재 공식 리비전 여부", example = "false")
        boolean currentReleased,
        @Schema(description = "리비전 작성자")
        PartUserSummaryResponse createdBy
) {
}
