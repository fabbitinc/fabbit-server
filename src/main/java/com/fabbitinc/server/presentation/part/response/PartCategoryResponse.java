package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부품 카테고리 응답")
public record PartCategoryResponse(
        @Schema(description = "카테고리 ID", example = "019d0000-0000-7000-8000-000000000001")
        UUID id,

        @Schema(description = "카테고리 이름", example = "PCB")
        String name,

        @Schema(description = "숫자 앞 포맷 문자열", example = "PCB-")
        String formatPrefix,

        @Schema(description = "숫자 뒤 포맷 문자열", example = "-A")
        String formatSuffix,

        @Schema(description = "자릿수", example = "4")
        int digits,

        @Schema(description = "자동채번 활성화 여부", example = "true")
        boolean autoNumberingEnabled,

        @Schema(description = "예시 품번", example = "PCB-0001")
        String previewPartNumber
) {
}
