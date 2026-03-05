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
        name = "change_request_issues",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_change_request_issues_cr_id_issue_id",
                        columnNames = {"change_request_id", "issue_id"}
                )
        },
        indexes = {
                @Index(name = "ix_change_request_issues_change_request_id", columnList = "change_request_id"),
                @Index(name = "ix_change_request_issues_issue_id", columnList = "issue_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeRequestIssue extends AbstractCreatedEntity {

    public static final String CODE_CR_ISSUE_CHANGE_REQUEST_REQUIRED = "CR_ISSUE_CHANGE_REQUEST_REQUIRED";
    public static final String CODE_CR_ISSUE_ISSUE_REQUIRED = "CR_ISSUE_ISSUE_REQUIRED";

    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", insertable = false, updatable = false)
    private ChangeRequest changeRequest;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    public ChangeRequestIssue(UUID changeRequestId, UUID issueId) {
        super(UuidV7Generator.next());
        this.changeRequestId = requireChangeRequestId(changeRequestId);
        this.issueId = requireIssueId(issueId);
    }

    public static ChangeRequestIssue link(UUID changeRequestId, UUID issueId) {
        return new ChangeRequestIssue(changeRequestId, issueId);
    }

    public static ChangeRequestIssue link(ChangeRequest changeRequest, Issue issue) {
        if (changeRequest == null) {
            throw new DomainException(CODE_CR_ISSUE_CHANGE_REQUEST_REQUIRED, "변경요청 ID는 필수입니다");
        }
        if (issue == null) {
            throw new DomainException(CODE_CR_ISSUE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        ChangeRequestIssue link = new ChangeRequestIssue(changeRequest.getId(), issue.getId());
        link.changeRequest = changeRequest;
        link.issue = issue;
        return link;
    }

    private UUID requireChangeRequestId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CR_ISSUE_CHANGE_REQUEST_REQUIRED, "변경요청 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CR_ISSUE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }
}
