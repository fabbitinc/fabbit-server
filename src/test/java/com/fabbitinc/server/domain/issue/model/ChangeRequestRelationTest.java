package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeRequestRelationTest {

    @Test
    void changeRequest_기본_관계컬렉션은_비어있다() {
        ChangeRequest changeRequest = new ChangeRequest(1, "CR 제목", "CR 본문", UUID.randomUUID());

        assertTrue(changeRequest.getLinkedIssues().isEmpty());
        assertTrue(changeRequest.getReviewers().isEmpty());
        assertTrue(changeRequest.getTeamReviewers().isEmpty());
    }

    @Test
    void changeRequestIssue_엔티티_입력시_FK와_연관을_동기화한다() {
        ChangeRequest changeRequest = new ChangeRequest(1, "CR 제목", "CR 본문", UUID.randomUUID());
        Issue issue = new Issue(2, "이슈 제목", "이슈 본문", UUID.randomUUID());

        ChangeRequestIssue link = ChangeRequestIssue.link(changeRequest, issue);

        assertEquals(changeRequest, link.getChangeRequest());
        assertEquals(issue, link.getIssue());
        assertEquals(changeRequest.getId(), link.getChangeRequestId());
        assertEquals(issue.getId(), link.getIssueId());
    }

    @Test
    void changeRequestReviewer_엔티티_입력시_FK와_연관을_동기화한다() {
        ChangeRequest changeRequest = new ChangeRequest(1, "CR 제목", "CR 본문", UUID.randomUUID());
        User user = new User("reviewer@example.com", "hashed", "Reviewer");

        ChangeRequestReviewer reviewer = ChangeRequestReviewer.assign(changeRequest, user);

        assertEquals(changeRequest, reviewer.getChangeRequest());
        assertEquals(user, reviewer.getUser());
        assertEquals(changeRequest.getId(), reviewer.getChangeRequestId());
        assertEquals(user.getId(), reviewer.getUserId());
        assertEquals(ReviewStatus.PENDING, reviewer.getReviewStatus());
    }

    @Test
    void changeRequestTeamReviewer_엔티티_입력시_FK와_연관을_동기화한다() {
        ChangeRequest changeRequest = new ChangeRequest(1, "CR 제목", "CR 본문", UUID.randomUUID());
        Team team = new Team("Review Team", null, UUID.randomUUID());

        ChangeRequestTeamReviewer reviewer = ChangeRequestTeamReviewer.assign(changeRequest, team);

        assertEquals(changeRequest, reviewer.getChangeRequest());
        assertEquals(team, reviewer.getTeam());
        assertEquals(changeRequest.getId(), reviewer.getChangeRequestId());
        assertEquals(team.getId(), reviewer.getTeamId());
    }
}
