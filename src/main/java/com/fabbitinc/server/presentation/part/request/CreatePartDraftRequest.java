package com.fabbitinc.server.presentation.part.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "부품 초안 생성 요청")
public record CreatePartDraftRequest(
        @Schema(description = "초안 생성 사유", example = "Rev 2 기반으로 고객 요청 변경안을 검토합니다")
        @Size(max = 2000, message = "reason은 최대 2000자여야 합니다") String reason
) {
}
