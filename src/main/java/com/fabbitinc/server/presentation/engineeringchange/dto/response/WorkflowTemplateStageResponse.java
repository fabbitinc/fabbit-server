package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "워크플로우 템플릿 단계 응답")
public record WorkflowTemplateStageResponse(
        @Schema(description = "단계 ID")
        UUID stageId,
        @Schema(description = "단계 타입")
        EngineeringChangeStepType stepType,
        @Schema(description = "단계 순서")
        int sequence,
        @Schema(description = "완료 정책")
        StepStageCompletionPolicy completionPolicy,
        @Schema(description = "최소 승인 수")
        Integer minApprovals,
        @Schema(description = "담당자 목록")
        List<AssigneeResponse> assignees
) {

    @Schema(description = "워크플로우 템플릿 단계 담당자 응답")
    public record AssigneeResponse(
            @Schema(description = "담당자 ID")
            UUID assigneeId,
            @Schema(description = "담당자 타입")
            EngineeringChangeStepAssigneeType assigneeType
    ) {
    }
}
