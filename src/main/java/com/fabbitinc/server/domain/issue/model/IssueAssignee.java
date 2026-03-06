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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "issue_assignees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_issue_assignees_issue_id_user_id", columnNames = {"issue_id", "user_id"})
        },
        indexes = {
                @Index(name = "ix_issue_assignees_issue_id", columnList = "issue_id"),
                @Index(name = "ix_issue_assignees_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueAssignee extends AbstractCreatedEntity {

    public static final String CODE_ISSUE_ASSIGNEE_ISSUE_REQUIRED = "ISSUE_ASSIGNEE_ISSUE_REQUIRED";
    public static final String CODE_ISSUE_ASSIGNEE_USER_REQUIRED = "ISSUE_ASSIGNEE_USER_REQUIRED";

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private IssueAssignee(UUID issueId, UUID userId) {
        super(UuidV7Generator.next());
        this.issueId = requireIssueId(issueId);
        this.userId = requireUserId(userId);
    }

    public static IssueAssignee assign(UUID issueId, UUID userId) {
        return new IssueAssignee(issueId, userId);
    }

    public static IssueAssignee assign(Issue issue, UUID userId) {
        if (issue == null) {
            throw new DomainException(CODE_ISSUE_ASSIGNEE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        if (userId == null) {
            throw new DomainException(CODE_ISSUE_ASSIGNEE_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        IssueAssignee assignee = new IssueAssignee(issue.getId(), userId);
        assignee.issue = issue;
        return assignee;
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_ASSIGNEE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_ASSIGNEE_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }
}
