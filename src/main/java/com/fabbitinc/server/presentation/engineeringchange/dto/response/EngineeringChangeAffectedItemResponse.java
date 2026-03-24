package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "변경관리 영향 항목")
public record EngineeringChangeAffectedItemResponse(
        @Schema(description = "영향 항목 ID")
        UUID id,
        @Schema(description = "항목 유형", example = "REVISION_RELEASE")
        EngineeringChangeAffectedItemType itemType,
        @Schema(description = "대상 ID (리비전 ID 또는 부품 ID)")
        UUID targetId,
        @Schema(description = "액션 상세 (JSON)")
        String actionDetail,
        @Schema(description = "부품 ID (리비전 릴리즈 시)")
        UUID partId,
        @Schema(description = "품번", example = "AES-100")
        String partNumber,
        @Schema(description = "리비전 코드", example = "1")
        String revisionCode,
        @Schema(description = "이름", example = "메인 하우징")
        String name,
        @Schema(description = "상태", example = "DRAFT")
        String status
) {
}
