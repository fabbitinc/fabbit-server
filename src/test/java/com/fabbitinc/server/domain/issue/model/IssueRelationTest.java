package com.fabbitinc.server.domain.issue.model;
import com.fabbitinc.server.domain.issue.model.IssueState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.part.model.Part;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IssueRelationTest {

    @Test
    void issue_기본_관계컬렉션은_비어있다() {
        Issue issue = Issue.create(1, "제목", "본문", UUID.randomUUID());

        assertTrue(issue.getAssignees().isEmpty());
        assertTrue(issue.getTeamAssignees().isEmpty());
        assertTrue(issue.getParts().isEmpty());
        assertTrue(issue.getLabels().isEmpty());
        assertTrue(issue.getComments().isEmpty());
    }

    @Test
    void issue_생성시_createdBy_updatedBy를_초기화한다() {
        UUID actorId = UUID.randomUUID();
        Issue issue = Issue.create(1, "제목", "본문", actorId);

        assertEquals(actorId, issue.getCreatedBy());
        assertEquals(actorId, issue.getUpdatedBy());
    }

    @Test
    void issue_제목은_trim_정규화한다() {
        Issue issue = Issue.create(1, "  제목  ", "본문", UUID.randomUUID());

        assertEquals("제목", issue.getTitle());
    }

    @Test
    void issue_제목이_blank면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Issue.create(1, "   ", "본문", UUID.randomUUID())
        );

        assertEquals(Issue.CODE_ISSUE_TITLE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void issue_assignUser_루트가_담당자를_생성한다() {
        Issue issue = Issue.create(1, "제목", "본문", UUID.randomUUID());
        UUID userId = UUID.randomUUID();

        IssueAssignee assignee = issue.assignUser(userId);

        assertEquals(issue, assignee.getIssue());
        assertEquals(issue.getId(), assignee.getIssueId());
        assertEquals(userId, assignee.getUserId());
    }

    @Test
    void issue_assignTeam_루트가_팀담당자를_생성한다() {
        Issue issue = Issue.create(1, "제목", "본문", UUID.randomUUID());
        UUID teamId = UUID.randomUUID();

        IssueTeamAssignee assignee = issue.assignTeam(teamId);

        assertEquals(issue, assignee.getIssue());
        assertEquals(issue.getId(), assignee.getIssueId());
        assertEquals(teamId, assignee.getTeamId());
    }

    @Test
    void issue_linkPart_루트가_파트링크를_생성한다() {
        Issue issue = Issue.create(1, "제목", "본문", UUID.randomUUID());
        Part part = Part.create("P-001");

        IssuePart issuePart = issue.linkPart(part.getId());

        assertEquals(issue, issuePart.getIssue());
        assertEquals(issue.getId(), issuePart.getIssueId());
        assertEquals(part.getId(), issuePart.getPartId());
    }

    @Test
    void issue_linkLabel_루트가_라벨링크를_생성한다() {
        Issue issue = Issue.create(1, "제목", "본문", UUID.randomUUID());
        Label label = Label.create("bug", null, "#ff0000", UUID.randomUUID());

        IssueLabel issueLabel = issue.linkLabel(label.getId());

        assertEquals(issue, issueLabel.getIssue());
        assertEquals(issue.getId(), issueLabel.getIssueId());
        assertEquals(label.getId(), issueLabel.getLabelId());
    }

    @Test
    void issue_writeComment_루트가_댓글을_생성한다() {
        Issue issue = Issue.create(1, "제목", "본문", UUID.randomUUID());
        UUID actorId = UUID.randomUUID();

        IssueComment comment = issue.writeComment("{\"type\":\"doc\"}", actorId);

        assertEquals(issue, comment.getIssue());
        assertEquals(issue.getId(), comment.getIssueId());
        assertEquals(actorId, comment.getCreatedBy());
        assertEquals(actorId, comment.getUpdatedBy());
    }

    @Test
    void issueComment_updateBody_수행자ID가_null이면_본문과_updatedBy를_유지한다() {
        UUID actorId = UUID.randomUUID();
        Issue issue = Issue.create(1, "제목", "본문", actorId);
        IssueComment comment = IssueComment.write(issue, "{\"type\":\"doc\"}", actorId);

        DomainException ex = assertThrows(DomainException.class, () -> comment.updateBody("{\"type\":\"text\"}", (UUID) null));

        assertEquals(IssueComment.CODE_ISSUE_COMMENT_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals("{\"type\":\"doc\"}", comment.getBody());
        assertEquals(actorId, comment.getUpdatedBy());
    }

    @Test
    void issue_updateBody_수행자ID가_null이면_본문과_updatedBy를_유지한다() {
        UUID actorId = UUID.randomUUID();
        Issue issue = Issue.create(1, "제목", "원본 본문", actorId);

        DomainException ex = assertThrows(DomainException.class, () -> issue.updateBody("변경 본문", (UUID) null));

        assertEquals(Issue.CODE_ISSUE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals("원본 본문", issue.getBody());
        assertEquals(actorId, issue.getUpdatedBy());
    }

    @Test
    void issue_close_수행자ID가_null이면_state와_closedAt을_유지한다() {
        UUID actorId = UUID.randomUUID();
        Issue issue = Issue.create(1, "제목", "본문", actorId);

        DomainException ex = assertThrows(DomainException.class, () -> issue.close(Instant.now(), (UUID) null));

        assertEquals(Issue.CODE_ISSUE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(IssueState.OPEN, issue.getState());
        assertEquals(null, issue.getClosedAt());
        assertEquals(actorId, issue.getUpdatedBy());
    }

    @Test
    void issue_reopen_수행자ID가_null이면_state와_closedAt을_유지한다() {
        UUID actorId = UUID.randomUUID();
        Issue issue = Issue.create(1, "제목", "본문", actorId);
        Instant closedAt = Instant.now();
        issue.close(closedAt, actorId);

        DomainException ex = assertThrows(DomainException.class, () -> issue.reopen((UUID) null));

        assertEquals(Issue.CODE_ISSUE_ACTOR_REQUIRED, ex.getDomainCode());
        assertEquals(IssueState.CLOSED, issue.getState());
        assertEquals(closedAt, issue.getClosedAt());
        assertEquals(actorId, issue.getUpdatedBy());
    }

    @Test
    void issue_close_이미_닫힌상태면_예외를_던진다() {
        UUID actorId = UUID.randomUUID();
        Issue issue = Issue.create(1, "제목", "본문", actorId);
        issue.close(Instant.now(), actorId);

        DomainException ex = assertThrows(DomainException.class, () -> issue.close(Instant.now(), actorId));

        assertEquals(Issue.CODE_ISSUE_INVALID_STATE, ex.getDomainCode());
    }

    @Test
    void issue_reopen_열린상태면_예외를_던진다() {
        Issue issue = Issue.create(1, "제목", "본문", UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () -> issue.reopen(UUID.randomUUID()));

        assertEquals(Issue.CODE_ISSUE_INVALID_STATE, ex.getDomainCode());
    }
}
