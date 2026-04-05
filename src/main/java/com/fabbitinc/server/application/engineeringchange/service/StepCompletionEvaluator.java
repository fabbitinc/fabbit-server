package com.fabbitinc.server.application.engineeringchange.service;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.StepStage;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stage 완료 정책 평가기.
 *
 * Truth Table:
 * ┌─────────────────┬──────────────┬──────────────┬──────────────┐
 * │ Event           │ ALL_MUST     │ ANY_ONE      │ MIN_N        │
 * ├─────────────────┼──────────────┼──────────────┼──────────────┤
 * │ APPROVED        │ 계속 대기    │ Stage 완료   │ count>=N:완료│
 * │ CHANGES_REQ     │ Stage 멈춤   │ Stage 멈춤   │ Stage 멈춤   │
 * │ REJECTED        │ Stage 실패   │ Stage 실패   │ Stage 실패   │
 * │ 완료후 남은     │ N/A          │ → CANCELED   │ → CANCELED   │
 * └─────────────────┴──────────────┴──────────────┴──────────────┘
 */
@Component
public class StepCompletionEvaluator {

    public StageEvaluationResult evaluate(StepStage stage, List<EngineeringChangeStep> stepsInStage) {
        // REJECTED는 모든 정책에서 거부권 (veto)
        boolean hasRejected = stepsInStage.stream()
                .anyMatch(s -> s.getStatus() == EngineeringChangeStepStatus.REJECTED);
        if (hasRejected) {
            return StageEvaluationResult.ofFailed();
        }

        // CHANGES_REQUESTED는 모든 정책에서 stage를 멈춤
        boolean hasChangesRequested = stepsInStage.stream()
                .anyMatch(s -> s.getStatus() == EngineeringChangeStepStatus.CHANGES_REQUESTED);
        if (hasChangesRequested) {
            return StageEvaluationResult.ofHalted();
        }

        long approvedCount = stepsInStage.stream()
                .filter(s -> s.getStatus() == EngineeringChangeStepStatus.APPROVED)
                .count();

        boolean complete = isComplete(stage.getCompletionPolicy(), stage.getMinApprovals(),
                approvedCount, stepsInStage.size());

        if (complete) {
            List<UUID> stepsToCancelIds = stepsInStage.stream()
                    .filter(EngineeringChangeStep::isPending)
                    .map(EngineeringChangeStep::getId)
                    .toList();
            return StageEvaluationResult.ofCompleted(stepsToCancelIds);
        }

        return StageEvaluationResult.ofPending();
    }

    private boolean isComplete(
            StepStageCompletionPolicy policy,
            Integer minApprovals,
            long approvedCount,
            int totalSteps
    ) {
        return switch (policy) {
            case ALL_MUST_APPROVE -> approvedCount == totalSteps;
            case ANY_ONE_APPROVES -> approvedCount >= 1;
            case MIN_N_APPROVES -> minApprovals != null && approvedCount >= minApprovals;
        };
    }
}
