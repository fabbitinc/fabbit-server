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

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public IssueAssignee(UUID issueId, UUID userId) {
        super(UuidV7Generator.next());
        this.issueId = issueId;
        this.userId = userId;
    }
}
