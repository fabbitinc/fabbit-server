package com.fabbitinc.server.domain.engineeringchange.model;

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
        name = "engineering_change_issues",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_engineering_change_issues_engineering_change_id_issue_id",
                        columnNames = {"engineering_change_id", "issue_id"}
                )
        },
        indexes = {
                @Index(name = "ix_engineering_change_issues_engineering_change_id", columnList = "engineering_change_id"),
                @Index(name = "ix_engineering_change_issues_issue_id", columnList = "issue_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringChangeIssueLink extends AbstractCreatedEntity {

    public static final String CODE_ENGINEERING_CHANGE_ISSUE_LINK_REQUIRED = "ENGINEERING_CHANGE_ISSUE_LINK_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_ISSUE_REQUIRED = "ENGINEERING_CHANGE_ISSUE_REQUIRED";

    @Column(name = "engineering_change_id", nullable = false)
    private UUID engineeringChangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineering_change_id", insertable = false, updatable = false)
    private EngineeringChange engineeringChange;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    private EngineeringChangeIssueLink(UUID engineeringChangeId, UUID issueId) {
        super(UuidV7Generator.next());
        this.engineeringChangeId = requireEngineeringChangeId(engineeringChangeId);
        this.issueId = requireIssueId(issueId);
    }

    public static EngineeringChangeIssueLink link(UUID engineeringChangeId, UUID issueId) {
        return new EngineeringChangeIssueLink(engineeringChangeId, issueId);
    }

    public static EngineeringChangeIssueLink link(EngineeringChange engineeringChange, UUID issueId) {
        if (engineeringChange == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_ISSUE_LINK_REQUIRED, "변경관리 ID는 필수입니다");
        }
        if (issueId == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        EngineeringChangeIssueLink link = new EngineeringChangeIssueLink(engineeringChange.getId(), issueId);
        link.engineeringChange = engineeringChange;
        return link;
    }

    private UUID requireEngineeringChangeId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_ISSUE_LINK_REQUIRED, "변경관리 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireIssueId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_ISSUE_REQUIRED, "이슈 ID는 필수입니다");
        }
        return value;
    }

    public UUID getEngineeringChangeId() {
        return engineeringChangeId;
    }

    public UUID getIssueId() {
        return issueId;
    }
}
