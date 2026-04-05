package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부품 카테고리 응답")
public record PartCategoryResponse(
        @Schema(description = "카테고리 ID", example = "019d0000-0000-7000-8000-000000000001")
        UUID id,

        @Schema(description = "카테고리 이름", example = "PCB")
        String name,

        @Schema(description = "카테고리 접두어", example = "PCB")
        String prefix,

        @Schema(description = "구분자", example = "-")
        String delimiter,

        @Schema(description = "자릿수", example = "4")
        int digits,

        @Schema(description = "예시 품번", example = "PCB-0001")
        String previewPartNumber
) {
}
