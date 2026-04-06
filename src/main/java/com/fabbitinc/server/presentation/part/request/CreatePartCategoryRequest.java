package com.fabbitinc.server.presentation.part.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "부품 카테고리 생성 요청")
public record CreatePartCategoryRequest(
        @Schema(description = "카테고리 이름", example = "PCB")
        @NotBlank(message = "name은 필수입니다") @Size(max = 100, message = "name은 최대 100자여야 합니다") String name,

        @Schema(description = "숫자 앞 포맷 문자열", example = "PCB-")
        @Size(max = 20, message = "formatPrefix는 최대 20자여야 합니다") String formatPrefix,

        @Schema(description = "숫자 뒤 포맷 문자열", example = "-A")
        @Size(max = 20, message = "formatSuffix는 최대 20자여야 합니다") String formatSuffix,

        @Schema(description = "자릿수", example = "4")
        @Max(value = 10, message = "digits는 10 이하여야 합니다") Integer digits,

        @Schema(description = "자동채번 활성화 여부", example = "true")
        @NotNull(message = "autoNumberingEnabled는 필수입니다") Boolean autoNumberingEnabled
) {
}
