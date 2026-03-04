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

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    public IssueLabel(UUID issueId, UUID labelId) {
        super(UuidV7Generator.next());
        this.issueId = issueId;
        this.labelId = labelId;
    }
}
