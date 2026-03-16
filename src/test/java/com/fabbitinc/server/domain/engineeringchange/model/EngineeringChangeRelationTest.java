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
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());

        assertTrue(engineeringChange.getLinkedIssues().isEmpty());
        assertTrue(engineeringChange.getSteps().isEmpty());
        assertTrue(engineeringChange.getComments().isEmpty());
    }

    @Test
    void engineeringChange_linkIssue_루트가_링크를_생성한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());
        Issue issue = Issue.create(2, "이슈 제목", "이슈 본문", UUID.randomUUID());

        EngineeringChangeIssueLink link = engineeringChange.linkIssue(issue.getId());

        assertEquals(engineeringChange, link.getEngineeringChange());
        assertEquals(engineeringChange.getId(), link.getEngineeringChangeId());
        assertEquals(issue.getId(), link.getIssueId());
    }

    @Test
    void engineeringChange_addStep_루트가_단계를_생성한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());
        UUID userId = UUID.randomUUID();

        EngineeringChangeStep step = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                userId,
                1,
                UUID.randomUUID()
        );

        assertEquals(engineeringChange.getId(), step.getEngineeringChangeId());
        assertEquals(EngineeringChangeStepType.REVIEW, step.getStepType());
        assertEquals(EngineeringChangeStepAssigneeType.USER, step.getAssigneeType());
        assertEquals(userId, step.getAssigneeId());
        assertEquals(EngineeringChangeStepStatus.PENDING, step.getStatus());
    }

    @Test
    void engineeringChangeStep_approve_정상상태면_상태와_시각을_갱신한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());
        EngineeringChangeStep step = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                1,
                UUID.randomUUID()
        );
        Instant actedAt = Instant.now();
        UUID actorId = UUID.randomUUID();

        step.approve(actorId, actedAt);

        assertEquals(EngineeringChangeStepStatus.APPROVED, step.getStatus());
        assertEquals(actorId, step.getActedBy());
        assertEquals(actedAt, step.getActedAt());
    }

    @Test
    void engineeringChangeStep_reject_정상상태면_반려상태와_시각을_갱신한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());
        EngineeringChangeStep step = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                1,
                UUID.randomUUID()
        );
        Instant actedAt = Instant.now();
        UUID actorId = UUID.randomUUID();

        step.reject(actorId, actedAt);

        assertEquals(EngineeringChangeStepStatus.REJECTED, step.getStatus());
        assertEquals(actorId, step.getActedBy());
        assertEquals(actedAt, step.getActedAt());
    }

    @Test
    void engineeringChangeStep_approve_이미처리된단계면_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());
        EngineeringChangeStep step = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                1,
                UUID.randomUUID()
        );
        step.approve(UUID.randomUUID(), Instant.now());

        DomainException ex = assertThrows(DomainException.class, () -> step.approve(UUID.randomUUID(), Instant.now()));

        assertEquals(EngineeringChangeStep.CODE_ENGINEERING_CHANGE_STEP_INVALID_STATUS, ex.getDomainCode());
    }

    @Test
    void engineeringChangeStep_approve_시각이없으면_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());
        EngineeringChangeStep step = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                UUID.randomUUID(),
                1,
                UUID.randomUUID()
        );

        DomainException ex = assertThrows(DomainException.class, () -> step.approve(UUID.randomUUID(), null));

        assertEquals(EngineeringChangeStep.CODE_ENGINEERING_CHANGE_STEP_ACTED_AT_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeStepStatus.PENDING, step.getStatus());
    }

    @Test
    void engineeringChange_writeComment_루트가_댓글을_생성한다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", actorId);

        EngineeringChangeComment comment = engineeringChange.writeComment("{\"type\":\"doc\"}", actorId);

        assertEquals(engineeringChange, comment.getEngineeringChange());
        assertEquals(engineeringChange.getId(), comment.getEngineeringChangeId());
        assertEquals(actorId, comment.getCreatedBy());
        assertEquals(actorId, comment.getUpdatedBy());
    }

    @Test
    void submit_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.submit(null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getState());
    }

    @Test
    void release_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", actorId);
        engineeringChange.submit(actorId);
        engineeringChange.completeReview(actorId);
        engineeringChange.approve(actorId);

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.release(Instant.now(), null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.RELEASE_PENDING, engineeringChange.getState());
    }

    @Test
    void cancel_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "변경 제목", "변경 본문", UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.cancel(Instant.now(), null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getState());
    }

}
