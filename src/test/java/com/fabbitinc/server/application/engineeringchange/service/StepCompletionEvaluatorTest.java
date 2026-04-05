package com.fabbitinc.server.application.engineeringchange.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStage;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StepCompletionEvaluatorTest {

    private StepCompletionEvaluator evaluator;
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        evaluator = new StepCompletionEvaluator();
    }

    private EngineeringChange createEc() {
        return EngineeringChange.create(1, "테스트 EC", null, null, actorId);
    }

    private StepStage createStage(EngineeringChange ec, StepStageCompletionPolicy policy, Integer minApprovals) {
        return ec.addStage(EngineeringChangeStepType.REVIEW, 1, policy, minApprovals, null, actorId);
    }

    private EngineeringChangeStep addStep(EngineeringChange ec, StepStage stage) {
        return ec.addStep(stage, EngineeringChangeStepAssigneeType.USER, UUID.randomUUID(), actorId);
    }

    // ── ALL_MUST_APPROVE ──

    @Nested
    @DisplayName("ALL_MUST_APPROVE 정책")
    class AllMustApproveTest {

        @Test
        @DisplayName("전원 승인 시 stage 완료")
        void allApproved_thenComplete() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ALL_MUST_APPROVE, null);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());
            step2.approve(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2));

            assertThat(result.complete()).isTrue();
            assertThat(result.halted()).isFalse();
            assertThat(result.failed()).isFalse();
            assertThat(result.stepsToCancelIds()).isEmpty();
        }

        @Test
        @DisplayName("일부만 승인 시 대기")
        void partialApproval_thenPending() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ALL_MUST_APPROVE, null);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2));

            assertThat(result.complete()).isFalse();
            assertThat(result.halted()).isFalse();
            assertThat(result.failed()).isFalse();
        }
    }

    // ── ANY_ONE_APPROVES ──

    @Nested
    @DisplayName("ANY_ONE_APPROVES 정책")
    class AnyOneApprovesTest {

        @Test
        @DisplayName("1명 승인 시 stage 완료, 나머지 PENDING은 취소 대상")
        void oneApproved_thenComplete_remainingCanceled() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ANY_ONE_APPROVES, null);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);
            EngineeringChangeStep step3 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2, step3));

            assertThat(result.complete()).isTrue();
            assertThat(result.stepsToCancelIds()).containsExactlyInAnyOrder(step2.getId(), step3.getId());
        }

        @Test
        @DisplayName("아무도 승인하지 않으면 대기")
        void noneApproved_thenPending() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ANY_ONE_APPROVES, null);
            EngineeringChangeStep step1 = addStep(ec, stage);

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1));

            assertThat(result.complete()).isFalse();
        }
    }

    // ── MIN_N_APPROVES ──

    @Nested
    @DisplayName("MIN_N_APPROVES 정책")
    class MinNApprovesTest {

        @Test
        @DisplayName("N명 이상 승인 시 stage 완료")
        void nApproved_thenComplete() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.MIN_N_APPROVES, 2);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);
            EngineeringChangeStep step3 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());
            step2.approve(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2, step3));

            assertThat(result.complete()).isTrue();
            assertThat(result.stepsToCancelIds()).containsExactly(step3.getId());
        }

        @Test
        @DisplayName("N명 미만 승인 시 대기")
        void lessThanN_thenPending() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.MIN_N_APPROVES, 2);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);
            EngineeringChangeStep step3 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2, step3));

            assertThat(result.complete()).isFalse();
        }
    }

    // ── 거부권 (모든 정책 공통) ──

    @Nested
    @DisplayName("REJECTED는 모든 정책에서 거부권")
    class RejectedVetoTest {

        @Test
        @DisplayName("ALL_MUST에서 1명 REJECTED → stage 실패")
        void allMust_rejected_thenFailed() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ALL_MUST_APPROVE, null);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());
            step2.reject(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2));

            assertThat(result.failed()).isTrue();
            assertThat(result.complete()).isFalse();
        }

        @Test
        @DisplayName("ANY_ONE에서 1명 REJECTED → stage 실패")
        void anyOne_rejected_thenFailed() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ANY_ONE_APPROVES, null);
            EngineeringChangeStep step1 = addStep(ec, stage);

            step1.reject(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1));

            assertThat(result.failed()).isTrue();
        }

        @Test
        @DisplayName("MIN_N에서 1명 REJECTED → stage 실패")
        void minN_rejected_thenFailed() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.MIN_N_APPROVES, 1);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());
            step2.reject(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2));

            assertThat(result.failed()).isTrue();
        }
    }

    // ── CHANGES_REQUESTED (모든 정책 공통) ──

    @Nested
    @DisplayName("CHANGES_REQUESTED는 모든 정책에서 stage 멈춤")
    class ChangesRequestedHaltTest {

        @Test
        @DisplayName("ALL_MUST에서 1명 CHANGES_REQUESTED → stage 멈춤")
        void allMust_changesRequested_thenHalted() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ALL_MUST_APPROVE, null);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());
            step2.requestChanges(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2));

            assertThat(result.halted()).isTrue();
            assertThat(result.complete()).isFalse();
            assertThat(result.failed()).isFalse();
        }

        @Test
        @DisplayName("ANY_ONE에서 승인 있어도 CHANGES_REQUESTED 있으면 멈춤")
        void anyOne_changesRequested_thenHalted() {
            EngineeringChange ec = createEc();
            StepStage stage = createStage(ec, StepStageCompletionPolicy.ANY_ONE_APPROVES, null);
            EngineeringChangeStep step1 = addStep(ec, stage);
            EngineeringChangeStep step2 = addStep(ec, stage);

            step1.approve(actorId, Instant.now());
            step2.requestChanges(actorId, Instant.now());

            StageEvaluationResult result = evaluator.evaluate(stage, List.of(step1, step2));

            assertThat(result.halted()).isTrue();
        }
    }
}
