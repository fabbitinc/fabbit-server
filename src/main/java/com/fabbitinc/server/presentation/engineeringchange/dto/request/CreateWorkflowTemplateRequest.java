package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "워크플로우 템플릿 생성 요청")
public record CreateWorkflowTemplateRequest(
        @NotBlank(message = "name은 필수입니다") @Size(max = 200, message = "name은 200자 이하여야 합니다")
        @Schema(description = "템플릿 이름", example = "기본 검토/승인 워크플로우")
        String name,
        @Schema(description = "템플릿 설명")
        String description,
        @NotNull(message = "stages는 필수입니다") @Valid
        @Schema(description = "단계 목록")
        List<StageRequest> stages
) {
    public CreateWorkflowTemplateRequest {
        stages = stages == null ? List.of() : List.copyOf(stages);
    }

    @Schema(description = "워크플로우 템플릿 단계 지정 요청")
    public record StageRequest(
            @NotNull(message = "stepType은 필수입니다") @Schema(description = "단계 타입", example = "REVIEW")
            EngineeringChangeStepType stepType,
            @Min(value = 1, message = "sequence는 1 이상이어야 합니다") @Schema(description = "단계 순서", example = "1")
            int sequence,
            @NotNull(message = "completionPolicy는 필수입니다") @Schema(description = "완료 정책", example = "ALL_MUST_APPROVE")
            StepStageCompletionPolicy completionPolicy,
            @Schema(description = "최소 승인 수 (MIN_N_APPROVES 정책에서 필수)")
            Integer minApprovals,
            @NotNull(message = "assignees는 필수입니다") @Valid @Schema(description = "담당자 목록")
            List<AssigneeRequest> assignees
    ) {
        public StageRequest {
            assignees = assignees == null ? List.of() : List.copyOf(assignees);
        }

        @Schema(description = "워크플로우 템플릿 단계 담당자 지정 요청")
        public record AssigneeRequest(
                @NotNull(message = "assigneeType은 필수입니다") @Schema(description = "담당자 타입", example = "USER")
                EngineeringChangeStepAssigneeType assigneeType,
                @NotNull(message = "assigneeId는 필수입니다") @Schema(description = "담당자 ID")
                UUID assigneeId
        ) {
        }
    }
}
