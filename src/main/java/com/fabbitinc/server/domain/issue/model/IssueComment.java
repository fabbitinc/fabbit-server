package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "issue_comments",
        indexes = {
                @Index(name = "ix_issue_comments_issue_id", columnList = "issue_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueComment extends AbstractAuditableEntity {

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    public IssueComment(UUID issueId, String body, UUID actorId) {
        super(UuidV7Generator.next());
        this.issueId = issueId;
        this.body = body;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void updateBody(String body, UUID actorId) {
        this.body = body;
        this.updatedBy = actorId;
    }
}
