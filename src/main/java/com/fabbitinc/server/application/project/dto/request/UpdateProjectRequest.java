package com.fabbitinc.server.application.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "프로젝트 수정 요청")
public record UpdateProjectRequest(
        @Schema(description = "변경할 프로젝트 이름", example = "신규 BOM 검토")
        @Size(min = 1, max = 200, message = "name은 1~200자여야 합니다") String name,
        @Schema(description = "변경할 프로젝트 설명", example = "설계 변경 대응 내용을 반영합니다")
        String description
) {
}
