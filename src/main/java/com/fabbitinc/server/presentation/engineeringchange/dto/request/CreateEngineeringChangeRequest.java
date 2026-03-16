package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "변경관리 생성 요청")
public record CreateEngineeringChangeRequest(
        @NotBlank @Size(max = 500) @Schema(description = "변경관리 제목")
        String title,
        @Schema(description = "변경관리 본문(TipTap JSON)")
        JsonNode body,
        @Schema(description = "연결할 원본 이슈 번호")
        Integer sourceIssueNumber,
        @Schema(description = "연결할 부품 초안 목록")
        @Valid
        List<EngineeringChangePartRevisionTargetRequest> partRevisions,
        @Schema(description = "첨부 파일 ID 목록(최대 20)")
        @Size(max = 20) List<UUID> fileIds,
        @Schema(description = "변경관리 단계 목록")
        @Valid
        List<EngineeringChangeStepRequest> steps
) {
    public CreateEngineeringChangeRequest {
        partRevisions = partRevisions == null ? List.of() : List.copyOf(partRevisions);
        fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
