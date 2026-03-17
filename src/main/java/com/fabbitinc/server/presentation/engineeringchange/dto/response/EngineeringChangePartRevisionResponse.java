package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "변경관리에 연결된 부품 초안")
public record EngineeringChangePartRevisionResponse(
        @Schema(description = "리비전 ID")
        UUID revisionId,
        @Schema(description = "부품 ID")
        UUID partId,
        @Schema(description = "품번", example = "AES-100")
        String partNumber,
        @Schema(description = "기준 공식 리비전 코드", example = "1")
        String baseRevisionCode,
        @Schema(description = "현재 리비전 코드", example = "2")
        String revisionCode,
        @Schema(description = "초안 이름", example = "메인 하우징")
        String name,
        @Schema(description = "초안 상태", example = "DRAFT")
        PartRevisionStatus status
) {
}
