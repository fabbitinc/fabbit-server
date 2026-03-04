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

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    public IssuePart(UUID issueId, UUID partId) {
        super(UuidV7Generator.next());
        this.issueId = issueId;
        this.partId = partId;
    }
}
