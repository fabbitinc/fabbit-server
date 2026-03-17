package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "subscription_change_requests", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionChangeRequest extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_CHANGE_REQUEST_SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_CHANGE_REQUEST_SUBSCRIPTION_REQUIRED";
    public static final String CODE_SUBSCRIPTION_CHANGE_REQUEST_EFFECTIVE_AT_REQUIRED = "SUBSCRIPTION_CHANGE_REQUEST_EFFECTIVE_AT_REQUIRED";
    public static final String CODE_SUBSCRIPTION_CHANGE_REQUEST_STATUS_REQUIRED = "SUBSCRIPTION_CHANGE_REQUEST_STATUS_REQUIRED";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subscription_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_change_requests_subscription_id")
    )
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_plan_type", length = 20)
    private WorkspacePlanType requestedPlanType;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionChangeRequestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    private SubscriptionChangeRequest(
            UUID subscriptionId,
            WorkspacePlanType requestedPlanType,
            Instant effectiveAt,
            SubscriptionChangeRequestStatus status,
            Map<String, Object> metadata
    ) {
        super(UuidV7Generator.next());
        this.subscriptionId = requireSubscriptionId(subscriptionId);
        this.requestedPlanType = requestedPlanType;
        this.effectiveAt = requireEffectiveAt(effectiveAt);
        this.status = requireStatus(status);
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static SubscriptionChangeRequest schedule(
            UUID subscriptionId,
            WorkspacePlanType requestedPlanType,
            Instant effectiveAt,
            Map<String, Object> metadata
    ) {
        return new SubscriptionChangeRequest(
                subscriptionId,
                requestedPlanType,
                effectiveAt,
                SubscriptionChangeRequestStatus.SCHEDULED,
                metadata
        );
    }

    public void markApplied() {
        this.status = SubscriptionChangeRequestStatus.APPLIED;
    }

    public void markCanceled() {
        this.status = SubscriptionChangeRequestStatus.CANCELED;
    }

    public void markFailed() {
        this.status = SubscriptionChangeRequestStatus.FAILED;
    }

    private UUID requireSubscriptionId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_CHANGE_REQUEST_SUBSCRIPTION_REQUIRED, "구독 ID는 필수입니다");
        }
        return value;
    }

    private Instant requireEffectiveAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_CHANGE_REQUEST_EFFECTIVE_AT_REQUIRED, "적용 시각은 필수입니다");
        }
        return value;
    }

    private SubscriptionChangeRequestStatus requireStatus(SubscriptionChangeRequestStatus value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_CHANGE_REQUEST_STATUS_REQUIRED, "변경 상태는 필수입니다");
        }
        return value;
    }
}
