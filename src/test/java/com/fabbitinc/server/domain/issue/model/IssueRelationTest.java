package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
