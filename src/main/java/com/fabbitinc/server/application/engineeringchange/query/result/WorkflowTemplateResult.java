package com.fabbitinc.server.application.engineeringchange.query.result;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowTemplateResult(
        UUID id,
        String name,
        String description,
        List<StageResult> stages,
        Instant createdAt
) {

    public record StageResult(
            UUID stageId,
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals,
            List<AssigneeResult> assignees
    ) {
    }

    public record AssigneeResult(
            UUID assigneeId,
            EngineeringChangeStepAssigneeType assigneeType
    ) {
    }
}
