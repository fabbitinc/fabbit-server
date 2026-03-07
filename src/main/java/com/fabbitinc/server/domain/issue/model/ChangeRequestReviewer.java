package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public static final String CODE_CR_REVIEWER_CHANGE_REQUEST_REQUIRED = "CR_REVIEWER_CHANGE_REQUEST_REQUIRED";
    public static final String CODE_CR_REVIEWER_USER_REQUIRED = "CR_REVIEWER_USER_REQUIRED";
    public static final String CODE_CR_REVIEWER_STATUS_REQUIRED = "CR_REVIEWER_STATUS_REQUIRED";
    public static final String CODE_CR_REVIEWER_INVALID_STATUS = "CR_REVIEWER_INVALID_STATUS";
    public static final String CODE_CR_REVIEWER_REVIEWED_AT_REQUIRED = "CR_REVIEWER_REVIEWED_AT_REQUIRED";

    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", insertable = false, updatable = false)
    private ChangeRequest changeRequest;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "review_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReviewStatus reviewStatus;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    private ChangeRequestReviewer(UUID changeRequestId, UUID userId) {
        super(UuidV7Generator.next());
        this.changeRequestId = requireChangeRequestId(changeRequestId);
        this.userId = requireUserId(userId);
        this.reviewStatus = ReviewStatus.PENDING;
    }

    public static ChangeRequestReviewer assign(UUID changeRequestId, UUID userId) {
        return new ChangeRequestReviewer(changeRequestId, userId);
    }

    public static ChangeRequestReviewer assign(ChangeRequest changeRequest, UUID userId) {
        if (changeRequest == null) {
            throw new DomainException(CODE_CR_REVIEWER_CHANGE_REQUEST_REQUIRED, "변경요청 ID는 필수입니다");
        }
        if (userId == null) {
            throw new DomainException(CODE_CR_REVIEWER_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        ChangeRequestReviewer reviewer = new ChangeRequestReviewer(changeRequest.getId(), userId);
        reviewer.changeRequest = changeRequest;
        return reviewer;
    }

    public void submit(ReviewStatus status, Instant reviewedAt) {
        ReviewStatus requiredStatus = requireReviewStatus(status);
        Instant requiredReviewedAt = requireReviewedAt(reviewedAt);
        this.reviewStatus = requiredStatus;
        this.reviewedAt = requiredReviewedAt;
    }

    private UUID requireChangeRequestId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CR_REVIEWER_CHANGE_REQUEST_REQUIRED, "변경요청 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CR_REVIEWER_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }

    private ReviewStatus requireReviewStatus(ReviewStatus value) {
        if (value == null) {
            throw new DomainException(CODE_CR_REVIEWER_STATUS_REQUIRED, "리뷰 상태는 필수입니다");
        }
        if (value == ReviewStatus.PENDING) {
            throw new DomainException(CODE_CR_REVIEWER_INVALID_STATUS, "리뷰 상태는 APPROVED 또는 REJECTED만 허용됩니다");
        }
        return value;
    }

    private Instant requireReviewedAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_CR_REVIEWER_REVIEWED_AT_REQUIRED, "리뷰 시각은 필수입니다");
        }
        return value;
    }
}
