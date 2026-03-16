package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.presentation.workitem.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "변경관리 단계 요약")
public record EngineeringChangeStepResponse(
        @Schema(description = "단계 ID")
        UUID stepId,
        @Schema(description = "단계 타입")
        EngineeringChangeStepType stepType,
        @Schema(description = "담당자 타입")
        EngineeringChangeStepAssigneeType assigneeType,
        @Schema(description = "단계 순서")
        int sequence,
        @Schema(description = "단계 처리 상태")
        EngineeringChangeStepStatus status,
        @Schema(description = "사용자 담당자")
        UserSummaryResponse assigneeUser,
        @Schema(description = "팀 담당자")
        TeamBadgeResponse assigneeTeam,
        @Schema(description = "실제 처리자")
        UserSummaryResponse actedBy,
        @Schema(description = "처리 시각")
        Instant actedAt
) {
}
