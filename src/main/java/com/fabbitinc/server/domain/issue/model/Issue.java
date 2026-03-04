package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "issues",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_issues_number", columnNames = "number")
        },
        indexes = {
                @Index(name = "ix_issues_type_state", columnList = "type,state"),
                @Index(name = "ix_issues_created_by", columnList = "created_by")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 20)
@DiscriminatorValue("ISSUE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends AbstractAuditableEntity {

    @Column(name = "number", nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, insertable = false, updatable = false)
    private IssueType type;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private IssueState state;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    public Issue(int number, String title, String body, UUID actorId) {
        super(UuidV7Generator.next());
        this.number = number;
        this.title = title;
        this.body = body;
        this.state = IssueState.OPEN;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void updateTitle(String title, UUID actorId) {
        this.title = title;
        this.updatedBy = actorId;
    }

    public void updateBody(String body, UUID actorId) {
        this.body = body;
        this.updatedBy = actorId;
    }

    public void close(Instant now, UUID actorId) {
        this.state = IssueState.CLOSED;
        this.closedAt = now;
        this.updatedBy = actorId;
    }

    public void reopen(UUID actorId) {
        this.state = IssueState.OPEN;
        this.closedAt = null;
        this.updatedBy = actorId;
    }

    protected void markClosed(Instant now, UUID actorId) {
        close(now, actorId);
    }

    protected void markOpen(UUID actorId) {
        reopen(actorId);
    }
}
