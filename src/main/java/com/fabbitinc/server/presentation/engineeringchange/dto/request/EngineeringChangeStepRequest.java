package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "변경관리 단계(Stage) 지정 요청")
public record EngineeringChangeStepRequest(
        @Schema(description = "기존 Stage ID. 기존 단계를 수정할 때 전달하고, 새 단계 추가 시 생략합니다")
        UUID stepStageId,
        @NotNull(message = "stepType은 필수입니다") @Schema(description = "단계 타입", example = "REVIEW")
        EngineeringChangeStepType stepType,
        @Min(value = 1, message = "sequence는 1 이상이어야 합니다") @Schema(description = "단계 순서", example = "1")
        int sequence,
        @NotNull(message = "completionPolicy는 필수입니다") @Schema(description = "완료 정책", example = "ALL_MUST_APPROVE")
        StepStageCompletionPolicy completionPolicy,
        @Schema(description = "최소 승인 수 (MIN_N_APPROVES 정책에서 필수)")
        Integer minApprovals,
        @Schema(description = "마감 기한")
        Instant deadline,
        @NotNull(message = "assignees는 필수입니다") @Valid @Schema(description = "담당자 목록")
        List<AssigneeRequest> assignees
) {
    public EngineeringChangeStepRequest {
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
    }

    @Schema(description = "단계 담당자 지정 요청")
    public record AssigneeRequest(
            @NotNull(message = "assigneeType은 필수입니다") @Schema(description = "담당자 타입", example = "USER")
            EngineeringChangeStepAssigneeType assigneeType,
            @NotNull(message = "assigneeId는 필수입니다") @Schema(description = "담당자 ID")
            UUID assigneeId
    ) {
    }
}
