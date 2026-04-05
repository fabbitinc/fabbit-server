package com.fabbitinc.server.domain.engineeringchange.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.issue.model.Issue;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EngineeringChangeRelationTest {

    @Test
    void engineeringChange_기본_관계컬렉션은_비어있다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());

        assertTrue(engineeringChange.getLinkedIssues().isEmpty());
        assertTrue(engineeringChange.getSteps().isEmpty());
        assertTrue(engineeringChange.getComments().isEmpty());
    }

    @Test
    void engineeringChange_linkIssue_루트가_링크를_생성한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());
        Issue issue = Issue.create(2, "이슈 제목", "이슈 본문", UUID.randomUUID());

        EngineeringChangeIssueLink link = engineeringChange.linkIssue(issue.getId());

        assertEquals(engineeringChange, link.getEngineeringChange());
        assertEquals(engineeringChange.getId(), link.getEngineeringChangeId());
        assertEquals(issue.getId(), link.getIssueId());
    }

    @Test
    void engineeringChange_addStep_루트가_단계를_생성한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        StepStage stage = engineeringChange.addStage(
                EngineeringChangeStepType.REVIEW,
                1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE,
                null,
                null,
                actorId
        );
        EngineeringChangeStep step = engineeringChange.addStep(
                stage,
                EngineeringChangeStepAssigneeType.USER,
                userId,
                actorId
        );

        assertEquals(engineeringChange.getId(), step.getEngineeringChangeId());
        assertEquals(EngineeringChangeStepType.REVIEW, stage.getStepType());
        assertEquals(EngineeringChangeStepAssigneeType.USER, step.getAssigneeType());
        assertEquals(userId, step.getAssigneeId());
        assertEquals(EngineeringChangeStepStatus.PENDING, step.getStatus());
    }

    @Test
    void engineeringChangeStep_approve_정상상태면_상태와_시각을_갱신한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());
        UUID actorId = UUID.randomUUID();
        StepStage stage = engineeringChange.addStage(
                EngineeringChangeStepType.REVIEW,
                1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE,
                null,
                null,
                actorId
        );
        EngineeringChangeStep step = engineeringChange.addStep(
                stage,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                actorId
        );
        Instant actedAt = Instant.now();
        UUID approver = UUID.randomUUID();

        step.approve(approver, actedAt);

        assertEquals(EngineeringChangeStepStatus.APPROVED, step.getStatus());
        assertEquals(approver, step.getActedBy());
        assertEquals(actedAt, step.getActedAt());
    }

    @Test
    void engineeringChangeStep_reject_정상상태면_반려상태와_시각을_갱신한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());
        UUID actorId = UUID.randomUUID();
        StepStage stage = engineeringChange.addStage(
                EngineeringChangeStepType.REVIEW,
                1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE,
                null,
                null,
                actorId
        );
        EngineeringChangeStep step = engineeringChange.addStep(
                stage,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                actorId
        );
        Instant actedAt = Instant.now();
        UUID rejector = UUID.randomUUID();

        step.reject(rejector, actedAt);

        assertEquals(EngineeringChangeStepStatus.REJECTED, step.getStatus());
        assertEquals(rejector, step.getActedBy());
        assertEquals(actedAt, step.getActedAt());
    }

    @Test
    void engineeringChangeStep_approve_이미처리된단계면_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());
        UUID actorId = UUID.randomUUID();
        StepStage stage = engineeringChange.addStage(
                EngineeringChangeStepType.REVIEW,
                1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE,
                null,
                null,
                actorId
        );
        EngineeringChangeStep step = engineeringChange.addStep(
                stage,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                actorId
        );
        step.approve(UUID.randomUUID(), Instant.now());

        DomainException ex = assertThrows(DomainException.class, () -> step.approve(UUID.randomUUID(), Instant.now()));

        assertEquals(EngineeringChangeStep.CODE_STEP_INVALID_STATUS, ex.getDomainCode());
    }

    @Test
    void engineeringChangeStep_approve_시각이없으면_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());
        UUID actorId = UUID.randomUUID();
        StepStage stage = engineeringChange.addStage(
                EngineeringChangeStepType.REVIEW,
                1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE,
                null,
                null,
                actorId
        );
        EngineeringChangeStep step = engineeringChange.addStep(
                stage,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                actorId
        );

        DomainException ex = assertThrows(DomainException.class, () -> step.approve(UUID.randomUUID(), null));

        assertEquals(EngineeringChangeStep.CODE_STEP_ACTED_AT_REQUIRED, ex.getDomainCode());
    }

    @Test
    void engineeringChange_writeComment_루트가_댓글을_생성한다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, actorId);

        EngineeringChangeComment comment = engineeringChange.writeComment("{\"type\":\"doc\"}", actorId);

        assertEquals(engineeringChange, comment.getEngineeringChange());
        assertEquals(engineeringChange.getId(), comment.getEngineeringChangeId());
        assertEquals(actorId, comment.getCreatedBy());
        assertEquals(actorId, comment.getUpdatedBy());
    }

    @Test
    void submit_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.submit(null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getState());
    }

    @Test
    void release_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, actorId);

        // REVIEW stage 추가 및 step 승인 → RELEASE_PENDING까지 전이
        StepStage reviewStage = engineeringChange.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep reviewStep = engineeringChange.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);
        reviewStep.approve(actorId, Instant.now());

        StepStage approvalStage = engineeringChange.addStage(
                EngineeringChangeStepType.APPROVAL, 2,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep approvalStep = engineeringChange.addStep(
                approvalStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);
        approvalStep.approve(actorId, Instant.now());

        engineeringChange.submit(actorId);

        // submit → syncStateFromStages → 모든 stage 완료 → RELEASE_PENDING
        assertEquals(EngineeringChangeState.RELEASE_PENDING, engineeringChange.getState());

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.release(Instant.now(), null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.RELEASE_PENDING, engineeringChange.getState());
    }

    @Test
    void cancel_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", null, UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.cancel(Instant.now(), null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getState());
    }

}
