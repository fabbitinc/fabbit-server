package com.fabbitinc.server.presentation.part.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "채번 카테고리 생성 요청")
public record CreatePartNumberCategoryRequest(
        @Schema(description = "카테고리 이름", example = "PCB")
        @NotBlank(message = "name은 필수입니다")
        @Size(max = 100, message = "name은 최대 100자여야 합니다")
        String name,

        @Schema(description = "채번 접두어", example = "PCB")
        @NotBlank(message = "prefix는 필수입니다")
        @Size(max = 20, message = "prefix는 최대 20자여야 합니다")
        String prefix,

        @Schema(description = "구분자", example = "-")
        @Size(max = 5, message = "delimiter는 최대 5자여야 합니다")
        String delimiter,

        @Schema(description = "자릿수", example = "4")
        @Min(value = 1, message = "digits는 1 이상이어야 합니다")
        @Max(value = 10, message = "digits는 10 이하여야 합니다")
        int digits
) {
}
