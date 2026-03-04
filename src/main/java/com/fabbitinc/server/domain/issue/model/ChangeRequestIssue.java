package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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

    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    public ChangeRequestIssue(UUID changeRequestId, UUID issueId) {
        super(UuidV7Generator.next());
        this.changeRequestId = changeRequestId;
        this.issueId = issueId;
    }
}
