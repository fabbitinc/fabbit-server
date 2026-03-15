package com.fabbitinc.server.presentation.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 부품 연결 결과 응답")
public record LinkPartsResponse(
        @Schema(description = "연결된 부품 수", example = "5")
        int linkedCount
) {
}
