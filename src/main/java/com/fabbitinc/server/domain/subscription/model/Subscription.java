package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.organization.model.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "subscriptions",
        schema = "public",
        indexes = {
                @Index(name = "ix_subscriptions_org_id", columnList = "org_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends AbstractAuditableEntity {

    public static final String CODE_SUBSCRIPTION_ORG_REQUIRED = "SUBSCRIPTION_ORG_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PLAN_TYPE_REQUIRED = "SUBSCRIPTION_PLAN_TYPE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_STATUS_REQUIRED = "SUBSCRIPTION_STATUS_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_START_REQUIRED = "SUBSCRIPTION_PERIOD_START_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_END_REQUIRED = "SUBSCRIPTION_PERIOD_END_REQUIRED";

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization organization;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Column(name = "ai_credits_granted", nullable = false)
    private int aiCreditsGranted;

    @Column(name = "storage_bytes_limit", nullable = false)
    private long storageBytesLimit;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    public Subscription(
            UUID orgId,
            String planType,
            SubscriptionStatus status,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            int maxMembers,
            int aiCreditsGranted,
            long storageBytesLimit
    ) {
        super(UuidV7Generator.next());
        this.orgId = requireOrgId(orgId);
        this.planType = requirePlanType(planType);
        this.status = requireStatus(status);
        this.currentPeriodStart = requirePeriodStart(currentPeriodStart);
        this.currentPeriodEnd = requirePeriodEnd(currentPeriodEnd);
        this.maxMembers = maxMembers;
        this.aiCreditsGranted = aiCreditsGranted;
        this.storageBytesLimit = storageBytesLimit;
        this.cancelAtPeriodEnd = false;
    }

    public static Subscription create(
            UUID orgId,
            String planType,
            SubscriptionStatus status,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            int maxMembers,
            int aiCreditsGranted,
            long storageBytesLimit
    ) {
        return new Subscription(
                orgId,
                planType,
                status,
                currentPeriodStart,
                currentPeriodEnd,
                maxMembers,
                aiCreditsGranted,
                storageBytesLimit
        );
    }

    public static Subscription create(
            Organization organization,
            String planType,
            SubscriptionStatus status,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            int maxMembers,
            int aiCreditsGranted,
            long storageBytesLimit
    ) {
        if (organization == null) {
            throw new DomainException(CODE_SUBSCRIPTION_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        Subscription subscription = new Subscription(
                organization.getId(),
                planType,
                status,
                currentPeriodStart,
                currentPeriodEnd,
                maxMembers,
                aiCreditsGranted,
                storageBytesLimit
        );
        subscription.organization = organization;
        return subscription;
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private String requirePlanType(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_SUBSCRIPTION_PLAN_TYPE_REQUIRED, "플랜 타입은 필수입니다");
        }
        return value;
    }

    private SubscriptionStatus requireStatus(SubscriptionStatus value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_STATUS_REQUIRED, "구독 상태는 필수입니다");
        }
        return value;
    }

    private Instant requirePeriodStart(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_PERIOD_START_REQUIRED, "구독 시작 시각은 필수입니다");
        }
        return value;
    }

    private Instant requirePeriodEnd(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_PERIOD_END_REQUIRED, "구독 종료 시각은 필수입니다");
        }
        return value;
    }
}
