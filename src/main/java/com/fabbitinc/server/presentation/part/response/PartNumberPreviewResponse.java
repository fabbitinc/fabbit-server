package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "다음 품번 미리보기 응답")
public record PartNumberPreviewResponse(
        @Schema(description = "예상 품번", example = "PCB-0042")
        String partNumber,

        @Schema(description = "안내 문구", example = "이 번호는 실제 생성 시 변경될 수 있습니다")
        String note
) {
}
