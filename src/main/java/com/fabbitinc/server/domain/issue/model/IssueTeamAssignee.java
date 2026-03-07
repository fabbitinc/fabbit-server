package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "issue_team_assignees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_issue_team_assignees_issue_id_team_id", columnNames = {"issue_id", "team_id"})
        },
        indexes = {
                @Index(name = "ix_issue_team_assignees_issue_id", columnList = "issue_id"),
                @Index(name = "ix_issue_team_assignees_team_id", columnList = "team_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueTeamAssignee extends AbstractCreatedEntity {

    public static final String CODE_ISSUE_TEAM_ASSIGNEE_ISSUE_REQUIRED = "ISSUE_TEAM_ASSIGNEE_ISSUE_REQUIRED";
    public static final String CODE_ISSUE_TEAM_ASSIGNEE_TEAM_REQUIRED = "ISSUE_TEAM_ASSIGNEE_TEAM_REQUIRED";

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    private IssueTeamAssignee(UUID issueId, UUID teamId) {
        super(UuidV7Generator.next());
        this.issueId = requireIssueId(issueId);
        this.teamId = requireTeamId(teamId);
    }

    public static IssueTeamAssignee assign(UUID issueId, UUID teamId) {
        return new IssueTeamAssignee(issueId, teamId);
    }

    public static IssueTeamAssignee assign(Issue issue, UUID teamId) {
        if (issue == null) {
            throw new DomainException(CODE_ISSUE_TEAM_ASSIGNEE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        if (teamId == null) {
            throw new DomainException(CODE_ISSUE_TEAM_ASSIGNEE_TEAM_REQUIRED, "팀 ID는 필수입니다");
        }
        IssueTeamAssignee assignee = new IssueTeamAssignee(issue.getId(), teamId);
        assignee.issue = issue;
        return assignee;
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_TEAM_ASSIGNEE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireTeamId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_TEAM_ASSIGNEE_TEAM_REQUIRED, "팀 ID는 필수입니다");
        }
        return value;
    }
}
