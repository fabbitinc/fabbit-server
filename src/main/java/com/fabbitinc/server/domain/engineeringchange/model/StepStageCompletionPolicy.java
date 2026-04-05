package com.fabbitinc.server.domain.engineeringchange.model;

/**
 * 단계(Stage) 완료 정책.
 * ALL_MUST_APPROVE: 모든 담당자 승인 필요
 * ANY_ONE_APPROVES: 1명 승인 시 완료
 * MIN_N_APPROVES: 최소 N명 승인 시 완료 (minApprovals 값 필요)
 */
public enum StepStageCompletionPolicy {
    ALL_MUST_APPROVE,
    ANY_ONE_APPROVES,
    MIN_N_APPROVES
}
