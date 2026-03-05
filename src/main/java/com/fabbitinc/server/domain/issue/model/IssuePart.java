package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.part.model.Part;
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
        name = "issue_parts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_issue_parts_issue_id_part_id", columnNames = {"issue_id", "part_id"})
        },
        indexes = {
                @Index(name = "ix_issue_parts_issue_id", columnList = "issue_id"),
                @Index(name = "ix_issue_parts_part_id", columnList = "part_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuePart extends AbstractCreatedEntity {

    public static final String CODE_ISSUE_PART_ISSUE_REQUIRED = "ISSUE_PART_ISSUE_REQUIRED";
    public static final String CODE_ISSUE_PART_PART_REQUIRED = "ISSUE_PART_PART_REQUIRED";

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", insertable = false, updatable = false)
    private Issue issue;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", insertable = false, updatable = false)
    private Part part;

    public IssuePart(UUID issueId, UUID partId) {
        super(UuidV7Generator.next());
        this.issueId = requireIssueId(issueId);
        this.partId = requirePartId(partId);
    }

    public static IssuePart link(UUID issueId, UUID partId) {
        return new IssuePart(issueId, partId);
    }

    public static IssuePart link(Issue issue, Part part) {
        if (issue == null) {
            throw new DomainException(CODE_ISSUE_PART_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        if (part == null) {
            throw new DomainException(CODE_ISSUE_PART_PART_REQUIRED, "부품 ID는 필수입니다");
        }
        IssuePart issuePart = new IssuePart(issue.getId(), part.getId());
        issuePart.issue = issue;
        issuePart.part = part;
        return issuePart;
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_PART_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }

    private UUID requirePartId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ISSUE_PART_PART_REQUIRED, "부품 ID는 필수입니다");
        }
        return value;
    }
}
