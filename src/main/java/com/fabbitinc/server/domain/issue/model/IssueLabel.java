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
        name = "issue_labels",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_issue_labels_issue_id_label_id", columnNames = {"issue_id", "label_id"})
        },
        indexes = {
                @Index(name = "ix_issue_labels_issue_id", columnList = "issue_id"),
                @Index(name = "ix_issue_labels_label_id", columnList = "label_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueLabel extends AbstractCreatedEntity {

    public static final String CODE_ISSUE_LABEL_ISSUE_REQUIRED = "ISSUE_LABEL_ISSUE_REQUIRED";
    public static final String CODE_ISSUE_LABEL_LABEL_REQUIRED = "ISSUE_LABEL_LABEL_REQUIRED";

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    private IssueLabel(UUID issueId, UUID labelId) {
        super(UuidV7Generator.next());
        this.issueId = requireIssueId(issueId);
        this.labelId = requireLabelId(labelId);
    }

    public static IssueLabel link(UUID issueId, UUID labelId) {
        return new IssueLabel(issueId, labelId);
    }

    public static IssueLabel link(Issue issue, UUID labelId) {
        if (issue == null) {
            throw new DomainException(CODE_ISSUE_LABEL_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        if (labelId == null) {
            throw new DomainException(CODE_ISSUE_LABEL_LABEL_REQUIRED, "라벨 ID는 필수입니다");
        }
        IssueLabel issueLabel = new IssueLabel(issue.getId(), labelId);
        issueLabel.issue = issue;
        return issueLabel;
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_LABEL_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireLabelId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_LABEL_LABEL_REQUIRED, "라벨 ID는 필수입니다");
        }
        return value;
    }
}
