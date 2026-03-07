package com.fabbitinc.server.application.synthesis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "합성 시작 요청")
public record SynthesisStartRequest(
        @Schema(description = "사용할 매핑 ID")
        @NotNull(message = "mapping_id는 필수입니다") UUID mappingId,
        @Schema(description = "대상 프로젝트 ID", nullable = true)
        UUID projectId,
        @Schema(description = "기존 데이터 덮어쓰기 여부", example = "false")
        boolean overwrite,
        @Schema(description = "합성할 업로드 파일 목록")
        @NotEmpty(message = "uploads는 최소 1개 이상이어야 합니다") @Size(max = 100, message = "uploads는 최대 100개까지 가능합니다") List<@Valid SynthesisUploadItem> uploads
) {
}
