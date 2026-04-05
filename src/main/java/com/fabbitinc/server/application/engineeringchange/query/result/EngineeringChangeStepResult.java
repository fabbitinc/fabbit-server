package com.fabbitinc.server.application.engineeringchange.query.result;

import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import java.time.Instant;
import java.util.UUID;

public record EngineeringChangeStepResult(
        UUID stepId,
        EngineeringChangeStepType stepType,
        EngineeringChangeStepAssigneeType assigneeType,
        int sequence,
        UUID stepStageId,
        StepStageCompletionPolicy completionPolicy,
        Instant deadline,
        EngineeringChangeStepStatus status,
        UserSummaryResult assigneeUser,
        TeamBadgeResult assigneeTeam,
        UserSummaryResult actedBy,
        Instant actedAt
) {
}
