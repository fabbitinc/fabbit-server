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

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "change_request_reviewers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_cr_reviewers_cr_id_user_id",
                        columnNames = {"change_request_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "ix_cr_reviewers_change_request_id", columnList = "change_request_id"),
                @Index(name = "ix_cr_reviewers_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeRequestReviewer extends AbstractCreatedEntity {

    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public ChangeRequestReviewer(UUID changeRequestId, UUID userId) {
        super(UuidV7Generator.next());
        this.changeRequestId = changeRequestId;
        this.userId = userId;
        this.reviewStatus = ReviewStatus.PENDING.name();
    }

    public void submit(ReviewStatus status, Instant reviewedAt) {
        this.reviewStatus = status.name();
        this.reviewedAt = reviewedAt;
    }
}
