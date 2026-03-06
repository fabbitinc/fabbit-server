package com.fabbitinc.server.application.label.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "요청 DTO")
public record CreateLabelRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(min = 1, max = 50, message = "name은 1~50자여야 합니다")
        String name,

        @Size(max = 200, message = "description은 최대 200자여야 합니다")
        String description,

        @NotBlank(message = "color는 필수입니다")
        @Pattern(
                regexp = "^#[0-9a-fA-F]{6}$",
                message = "color는 #RRGGBB 형식이어야 합니다"
        )
        String color
) {
}
