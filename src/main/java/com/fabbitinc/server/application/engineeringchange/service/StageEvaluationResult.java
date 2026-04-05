package com.fabbitinc.server.application.engineeringchange.service;

import java.util.List;
import java.util.UUID;

/**
 * Stage 완료 평가 결과.
 *
 * @param complete stage가 완료되었는지 (승인 조건 충족)
 * @param halted   CHANGES_REQUESTED로 stage가 멈췄는지
 * @param failed   REJECTED로 stage가 실패했는지 (전체 리셋 필요)
 * @param stepsToCancelIds stage 완료 시 취소해야 할 남은 PENDING step ID 목록
 */
public record StageEvaluationResult(
        boolean complete,
        boolean halted,
        boolean failed,
        List<UUID> stepsToCancelIds
) {

    public static StageEvaluationResult ofCompleted(List<UUID> stepsToCancelIds) {
        return new StageEvaluationResult(true, false, false, stepsToCancelIds);
    }

    public static StageEvaluationResult ofHalted() {
        return new StageEvaluationResult(false, true, false, List.of());
    }

    public static StageEvaluationResult ofFailed() {
        return new StageEvaluationResult(false, false, true, List.of());
    }

    public static StageEvaluationResult ofPending() {
        return new StageEvaluationResult(false, false, false, List.of());
    }
}
