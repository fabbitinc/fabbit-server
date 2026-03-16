package com.fabbitinc.server.domain.issue.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EngineeringChangeRelationTest {

    @Test
    void engineeringChange_기본_관계컬렉션은_비어있다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());

        assertTrue(engineeringChange.getLinkedIssues().isEmpty());
        assertTrue(engineeringChange.getReviewers().isEmpty());
        assertTrue(engineeringChange.getTeamReviewers().isEmpty());
        assertTrue(engineeringChange.getComments().isEmpty());
    }

    @Test
    void engineeringChange_linkIssue_루트가_링크를_생성한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        Issue issue = Issue.create(2, "이슈 제목", "이슈 본문", UUID.randomUUID());

        EngineeringChangeIssueLink link = engineeringChange.linkIssue(issue.getId());

        assertEquals(engineeringChange, link.getEngineeringChange());
        assertEquals(engineeringChange.getId(), link.getEngineeringChangeId());
        assertEquals(issue.getId(), link.getIssueId());
    }

    @Test
    void engineeringChange_assignReviewer_루트가_검토자를_생성한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        UUID userId = UUID.randomUUID();

        EngineeringChangeReviewer reviewer = engineeringChange.assignReviewer(userId);

        assertEquals(engineeringChange, reviewer.getEngineeringChange());
        assertEquals(engineeringChange.getId(), reviewer.getEngineeringChangeId());
        assertEquals(userId, reviewer.getUserId());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
    }

    @Test
    void engineeringChangeReviewer_submit_정상상태면_리뷰상태와_시각을_갱신한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        EngineeringChangeReviewer reviewer = engineeringChange.assignReviewer(UUID.randomUUID());
        Instant reviewedAt = Instant.now();

        reviewer.submit(ReviewStatus.APPROVED, reviewedAt);

        assertEquals(ReviewStatus.APPROVED, reviewer.getReviewStatus());
        assertEquals(reviewedAt, reviewer.getReviewedAt());
    }

    @Test
    void engineeringChangeReviewer_submit_pending이면_예외를_던지고_기존상태를_유지한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        EngineeringChangeReviewer reviewer = engineeringChange.assignReviewer(UUID.randomUUID());
        Instant reviewedAt = Instant.now();

        DomainException ex = assertThrows(DomainException.class, () -> reviewer.submit(ReviewStatus.PENDING, reviewedAt));

        assertEquals(EngineeringChangeReviewer.CODE_ENGINEERING_CHANGE_REVIEWER_INVALID_STATUS, ex.getDomainCode());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
        assertEquals(null, reviewer.getReviewedAt());
    }

    @Test
    void engineeringChangeReviewer_submit_status_null이면_예외를_던지고_기존상태를_유지한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        EngineeringChangeReviewer reviewer = engineeringChange.assignReviewer(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> reviewer.submit(null, Instant.now()));

        assertEquals(EngineeringChangeReviewer.CODE_ENGINEERING_CHANGE_REVIEWER_STATUS_REQUIRED, ex.getDomainCode());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
        assertEquals(null, reviewer.getReviewedAt());
    }

    @Test
    void engineeringChangeReviewer_submit_reviewedAt_null이면_예외를_던지고_기존상태를_유지한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        EngineeringChangeReviewer reviewer = engineeringChange.assignReviewer(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> reviewer.submit(ReviewStatus.REJECTED, null));

        assertEquals(
                EngineeringChangeReviewer.CODE_ENGINEERING_CHANGE_REVIEWER_REVIEWED_AT_REQUIRED,
                ex.getDomainCode()
        );
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
        assertEquals(null, reviewer.getReviewedAt());
    }

    @Test
    void engineeringChange_assignTeamReviewer_루트가_팀검토자를_생성한다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        UUID teamId = UUID.randomUUID();

        EngineeringChangeTeamReviewer reviewer = engineeringChange.assignTeamReviewer(teamId);

        assertEquals(engineeringChange, reviewer.getEngineeringChange());
        assertEquals(engineeringChange.getId(), reviewer.getEngineeringChangeId());
        assertEquals(teamId, reviewer.getTeamId());
    }

    @Test
    void engineeringChange_writeComment_루트가_댓글을_생성한다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", actorId);

        EngineeringChangeComment comment = engineeringChange.writeComment("{\"type\":\"doc\"}", actorId);

        assertEquals(engineeringChange, comment.getEngineeringChange());
        assertEquals(engineeringChange.getId(), comment.getEngineeringChangeId());
        assertEquals(actorId, comment.getCreatedBy());
        assertEquals(actorId, comment.getUpdatedBy());
    }

    @Test
    void submit_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.submit(null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getEngineeringChangeState());
        assertEquals(IssueState.OPEN, engineeringChange.getState());
    }

    @Test
    void merge_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", actorId);
        engineeringChange.submit(actorId);

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.merge(Instant.now(), null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.SUBMITTED, engineeringChange.getEngineeringChangeState());
        assertEquals(IssueState.OPEN, engineeringChange.getState());
    }

    @Test
    void close_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.close(Instant.now(), null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getEngineeringChangeState());
        assertEquals(IssueState.OPEN, engineeringChange.getState());
    }

    @Test
    void reopen_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "CR 제목", "CR 본문", actorId);
        engineeringChange.close(Instant.now(), actorId);

        DomainException ex = assertThrows(DomainException.class, () -> engineeringChange.reopen(null));

        assertEquals(EngineeringChange.CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(EngineeringChangeState.CLOSED, engineeringChange.getEngineeringChangeState());
        assertEquals(IssueState.CLOSED, engineeringChange.getState());
    }
}
