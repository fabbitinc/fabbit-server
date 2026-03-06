package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeRequestRelationTest {

    @Test
    void changeRequest_기본_관계컬렉션은_비어있다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());

        assertTrue(changeRequest.getLinkedIssues().isEmpty());
        assertTrue(changeRequest.getReviewers().isEmpty());
        assertTrue(changeRequest.getTeamReviewers().isEmpty());
    }

    @Test
    void changeRequest_linkIssue_루트가_링크를_생성한다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        Issue issue = Issue.create(2, "이슈 제목", "이슈 본문", UUID.randomUUID());

        ChangeRequestIssue link = changeRequest.linkIssue(issue.getId());

        assertEquals(changeRequest, link.getChangeRequest());
        assertEquals(changeRequest.getId(), link.getChangeRequestId());
        assertEquals(issue.getId(), link.getIssueId());
    }

    @Test
    void changeRequest_assignReviewer_루트가_검토자를_생성한다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        UUID userId = UUID.randomUUID();

        ChangeRequestReviewer reviewer = changeRequest.assignReviewer(userId);

        assertEquals(changeRequest, reviewer.getChangeRequest());
        assertEquals(changeRequest.getId(), reviewer.getChangeRequestId());
        assertEquals(userId, reviewer.getUserId());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
    }

    @Test
    void changeRequestReviewer_submit_정상상태면_리뷰상태와_시각을_갱신한다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        ChangeRequestReviewer reviewer = changeRequest.assignReviewer(UUID.randomUUID());
        Instant reviewedAt = Instant.now();

        reviewer.submit(ReviewStatus.APPROVED, reviewedAt);

        assertEquals(ReviewStatus.APPROVED, reviewer.getReviewStatus());
        assertEquals(reviewedAt, reviewer.getReviewedAt());
    }

    @Test
    void changeRequestReviewer_submit_pending이면_예외를_던지고_기존상태를_유지한다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        ChangeRequestReviewer reviewer = changeRequest.assignReviewer(UUID.randomUUID());
        Instant reviewedAt = Instant.now();

        DomainException ex = assertThrows(DomainException.class, () -> reviewer.submit(ReviewStatus.PENDING, reviewedAt));

        assertEquals(ChangeRequestReviewer.CODE_CR_REVIEWER_INVALID_STATUS, ex.getDomainCode());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
        assertEquals(null, reviewer.getReviewedAt());
    }

    @Test
    void changeRequestReviewer_submit_status_null이면_예외를_던지고_기존상태를_유지한다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        ChangeRequestReviewer reviewer = changeRequest.assignReviewer(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> reviewer.submit(null, Instant.now()));

        assertEquals(ChangeRequestReviewer.CODE_CR_REVIEWER_STATUS_REQUIRED, ex.getDomainCode());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
        assertEquals(null, reviewer.getReviewedAt());
    }

    @Test
    void changeRequestReviewer_submit_reviewedAt_null이면_예외를_던지고_기존상태를_유지한다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        ChangeRequestReviewer reviewer = changeRequest.assignReviewer(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> reviewer.submit(ReviewStatus.REJECTED, null));

        assertEquals(ChangeRequestReviewer.CODE_CR_REVIEWER_REVIEWED_AT_REQUIRED, ex.getDomainCode());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
        assertEquals(null, reviewer.getReviewedAt());
    }

    @Test
    void changeRequest_assignTeamReviewer_루트가_팀검토자를_생성한다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());
        UUID teamId = UUID.randomUUID();

        ChangeRequestTeamReviewer reviewer = changeRequest.assignTeamReviewer(teamId);

        assertEquals(changeRequest, reviewer.getChangeRequest());
        assertEquals(changeRequest.getId(), reviewer.getChangeRequestId());
        assertEquals(teamId, reviewer.getTeamId());
    }

    @Test
    void submit_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> changeRequest.submit(null));

        assertEquals(Issue.CODE_ISSUE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(CrState.DRAFT, changeRequest.getCrState());
        assertEquals(IssueState.OPEN, changeRequest.getState());
    }

    @Test
    void merge_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", actorId);
        changeRequest.submit(actorId);

        DomainException ex = assertThrows(DomainException.class, () -> changeRequest.merge(Instant.now(), null));

        assertEquals(Issue.CODE_ISSUE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(CrState.SUBMITTED, changeRequest.getCrState());
        assertEquals(IssueState.OPEN, changeRequest.getState());
    }

    @Test
    void closeCr_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> changeRequest.closeCr(Instant.now(), null));

        assertEquals(Issue.CODE_ISSUE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(CrState.DRAFT, changeRequest.getCrState());
        assertEquals(IssueState.OPEN, changeRequest.getState());
    }

    @Test
    void reopenCr_수행자ID가_null이면_상태변경없이_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        ChangeRequest changeRequest = ChangeRequest.create(1, "CR 제목", "CR 본문", actorId);
        changeRequest.closeCr(Instant.now(), actorId);

        DomainException ex = assertThrows(DomainException.class, () -> changeRequest.reopenCr(null));

        assertEquals(Issue.CODE_ISSUE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(CrState.CLOSED, changeRequest.getCrState());
        assertEquals(IssueState.CLOSED, changeRequest.getState());
    }
}
