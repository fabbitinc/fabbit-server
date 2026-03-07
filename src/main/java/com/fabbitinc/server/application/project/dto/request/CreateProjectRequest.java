package com.fabbitinc.server.application.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "프로젝트 생성 요청")
public record CreateProjectRequest(
        @Schema(description = "프로젝트 이름", example = "신규 BOM 검토")
        @NotBlank(message = "name은 필수입니다") @Size(min = 1, max = 200, message = "name은 1~200자여야 합니다") String name,
        @Schema(description = "프로젝트 설명", example = "2026년 1분기 양산 준비 프로젝트")
        String description
) {
}
