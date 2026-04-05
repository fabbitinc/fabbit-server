package com.fabbitinc.server.application.engineeringchange.query.result;

import java.util.UUID;

/**
 * EC별 단계(Stage) 진행 현황 조회 결과
 */
public record StepProgressResult(
        UUID engineeringChangeId,
        int totalStages,
        int completedStages,
        String currentStageType,
        int currentStageStepsTotal,
        int currentStageStepsApproved,
        int currentStageStepsPending,
        int currentStageStepsChangesRequested
) {
}
