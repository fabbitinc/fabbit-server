package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueRelationTest {

    @Test
    void issue_기본_관계컬렉션은_비어있다() {
        Issue issue = new Issue(1, "제목", "본문", UUID.randomUUID());

        assertTrue(issue.getAssignees().isEmpty());
        assertTrue(issue.getTeamAssignees().isEmpty());
        assertTrue(issue.getParts().isEmpty());
        assertTrue(issue.getLabels().isEmpty());
        assertTrue(issue.getComments().isEmpty());
        assertTrue(issue.getLinkedChangeRequests().isEmpty());
    }

    @Test
    void issue_엔티티_입력시_createdBy_updatedBy_FK와_연관을_동기화한다() {
        User actor = new User("actor@example.com", "hashed", "Actor");

        Issue issue = Issue.create(1, "제목", "본문", actor);

        assertEquals(actor.getId(), issue.getCreatedBy());
        assertEquals(actor, issue.getCreatedByUser());
        assertEquals(actor.getId(), issue.getUpdatedBy());
        assertEquals(actor, issue.getUpdatedByUser());
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
    void issueAssignee_엔티티_입력시_FK와_연관을_동기화한다() {
        Issue issue = new Issue(1, "제목", "본문", UUID.randomUUID());
        User user = new User("issue-user@example.com", "hashed", "Issue User");

        IssueAssignee assignee = IssueAssignee.assign(issue, user);

        assertEquals(issue, assignee.getIssue());
        assertEquals(user, assignee.getUser());
        assertEquals(issue.getId(), assignee.getIssueId());
        assertEquals(user.getId(), assignee.getUserId());
    }

    @Test
    void issueTeamAssignee_엔티티_입력시_FK와_연관을_동기화한다() {
        Issue issue = new Issue(1, "제목", "본문", UUID.randomUUID());
        Team team = new Team("QA Team", null, UUID.randomUUID());

        IssueTeamAssignee assignee = IssueTeamAssignee.assign(issue, team);

        assertEquals(issue, assignee.getIssue());
        assertEquals(team, assignee.getTeam());
        assertEquals(issue.getId(), assignee.getIssueId());
        assertEquals(team.getId(), assignee.getTeamId());
    }

    @Test
    void issuePart_엔티티_입력시_FK와_연관을_동기화한다() {
        Issue issue = new Issue(1, "제목", "본문", UUID.randomUUID());
        Part part = Part.create("P-001", "Bolt");

        IssuePart issuePart = IssuePart.link(issue, part);

        assertEquals(issue, issuePart.getIssue());
        assertEquals(part, issuePart.getPart());
        assertEquals(issue.getId(), issuePart.getIssueId());
        assertEquals(part.getId(), issuePart.getPartId());
    }

    @Test
    void issueLabel_엔티티_입력시_FK와_연관을_동기화한다() {
        Issue issue = new Issue(1, "제목", "본문", UUID.randomUUID());
        Label label = new Label("bug", null, "#ff0000", UUID.randomUUID());

        IssueLabel issueLabel = IssueLabel.link(issue, label);

        assertEquals(issue, issueLabel.getIssue());
        assertEquals(label, issueLabel.getLabel());
        assertEquals(issue.getId(), issueLabel.getIssueId());
        assertEquals(label.getId(), issueLabel.getLabelId());
    }

    @Test
    void issueComment_엔티티_입력시_FK와_연관을_동기화한다() {
        Issue issue = new Issue(1, "제목", "본문", UUID.randomUUID());
        UUID actorId = UUID.randomUUID();

        IssueComment comment = IssueComment.write(issue, "{\"type\":\"doc\"}", actorId);

        assertEquals(issue, comment.getIssue());
        assertEquals(issue.getId(), comment.getIssueId());
        assertEquals(actorId, comment.getCreatedBy());
        assertEquals(actorId, comment.getUpdatedBy());
    }

    @Test
    void issueComment_엔티티_입력시_actor_FK와_연관을_동기화한다() {
        Issue issue = new Issue(1, "제목", "본문", UUID.randomUUID());
        User actor = new User("commenter@example.com", "hashed", "Commenter");

        IssueComment comment = IssueComment.write(issue, "{\"type\":\"doc\"}", actor);

        assertEquals(issue, comment.getIssue());
        assertEquals(issue.getId(), comment.getIssueId());
        assertEquals(actor.getId(), comment.getCreatedBy());
        assertEquals(actor, comment.getCreatedByUser());
        assertEquals(actor.getId(), comment.getUpdatedBy());
        assertEquals(actor, comment.getUpdatedByUser());
    }

    @Test
    void issueComment_updateBody_수행자ID가_null이면_본문과_updatedBy를_유지한다() {
        UUID actorId = UUID.randomUUID();
        Issue issue = new Issue(1, "제목", "본문", actorId);
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
}
