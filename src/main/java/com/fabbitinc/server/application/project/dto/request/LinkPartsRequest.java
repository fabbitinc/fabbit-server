package com.fabbitinc.server.application.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Schema(description = "프로젝트 부품 연결 요청")
public record LinkPartsRequest(
        @Schema(description = "연결 또는 해제할 부품 ID 목록")
        @NotEmpty(message = "part_ids는 1개 이상이어야 합니다") List<UUID> partIds
) {
}
