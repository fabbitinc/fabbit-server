package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.organization.model.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "subscriptions",
        schema = "public",
        indexes = {
                @Index(name = "ix_subscriptions_org_id", columnList = "org_id"),
                @Index(name = "ix_subscriptions_plan_type_status", columnList = "plan_type,status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_ORG_REQUIRED = "SUBSCRIPTION_ORG_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PLAN_TYPE_REQUIRED = "SUBSCRIPTION_PLAN_TYPE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_STATUS_REQUIRED = "SUBSCRIPTION_STATUS_REQUIRED";
    public static final String CODE_SUBSCRIPTION_BILLING_CYCLE_REQUIRED = "SUBSCRIPTION_BILLING_CYCLE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_START_REQUIRED = "SUBSCRIPTION_PERIOD_START_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_END_REQUIRED = "SUBSCRIPTION_PERIOD_END_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_INVALID = "SUBSCRIPTION_PERIOD_INVALID";
    public static final String CODE_SUBSCRIPTION_INVALID_STATE = "SUBSCRIPTION_INVALID_STATE";

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "org_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscriptions_org_id")
    )
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private WorkspacePlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduled_plan_type", length = 20)
    private WorkspacePlanType scheduledPlanType;

    @Column(name = "scheduled_change_effective_at")
    private Instant scheduledChangeEffectiveAt;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    private Subscription(
            UUID orgId,
            WorkspacePlanType planType,
            SubscriptionStatus status,
            BillingCycle billingCycle,
            Instant currentPeriodStart,
            Instant currentPeriodEnd
    ) {
        super(UuidV7Generator.next());
        this.orgId = requireOrgId(orgId);
        this.planType = requirePlanType(planType);
        this.status = requireStatus(status);
        this.billingCycle = requireBillingCycle(billingCycle);
        this.currentPeriodStart = requirePeriodStart(currentPeriodStart);
        this.currentPeriodEnd = requirePeriodEnd(currentPeriodEnd);
        validatePeriodRange(this.currentPeriodStart, this.currentPeriodEnd);
        this.cancelAtPeriodEnd = false;
    }

    public static Subscription create(
            UUID orgId,
            WorkspacePlanType planType,
            SubscriptionStatus status,
            BillingCycle billingCycle,
            Instant currentPeriodStart,
            Instant currentPeriodEnd
    ) {
        return new Subscription(orgId, planType, status, billingCycle, currentPeriodStart, currentPeriodEnd);
    }

    public void changePlan(WorkspacePlanType planType) {
        this.planType = requirePlanType(planType);
        this.scheduledPlanType = null;
        this.scheduledChangeEffectiveAt = null;
    }

    public void schedulePlanChange(WorkspacePlanType planType, Instant effectiveAt) {
        this.scheduledPlanType = requirePlanType(planType);
        this.scheduledChangeEffectiveAt = requirePeriodEnd(effectiveAt);
    }

    public void clearScheduledPlanChange() {
        this.scheduledPlanType = null;
        this.scheduledChangeEffectiveAt = null;
    }

    public void renew(Instant currentPeriodStart, Instant currentPeriodEnd) {
        validateRenewable();
        this.currentPeriodStart = requirePeriodStart(currentPeriodStart);
        this.currentPeriodEnd = requirePeriodEnd(currentPeriodEnd);
        validatePeriodRange(this.currentPeriodStart, this.currentPeriodEnd);
        this.status = SubscriptionStatus.ACTIVE;
        this.cancelAtPeriodEnd = false;
    }

    public void markPastDue() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new DomainException(CODE_SUBSCRIPTION_INVALID_STATE, "활성 구독만 연체 상태로 변경할 수 있습니다");
        }
        this.status = SubscriptionStatus.PAST_DUE;
    }

    public void scheduleCancelAtPeriodEnd() {
        validateCancelable();
        this.cancelAtPeriodEnd = true;
    }

    public void keepRenewing() {
        validateCancelable();
        this.cancelAtPeriodEnd = false;
    }

    public void cancel() {
        if (status == SubscriptionStatus.CANCELED) {
            return;
        }
        if (status == SubscriptionStatus.EXPIRED) {
            throw new DomainException(CODE_SUBSCRIPTION_INVALID_STATE, "만료된 구독은 해지할 수 없습니다");
        }
        this.status = SubscriptionStatus.CANCELED;
        this.cancelAtPeriodEnd = false;
        clearScheduledPlanChange();
    }

    public void expire() {
        if (status == SubscriptionStatus.EXPIRED) {
            return;
        }
        if (status == SubscriptionStatus.CANCELED) {
            throw new DomainException(CODE_SUBSCRIPTION_INVALID_STATE, "해지된 구독은 만료 처리할 수 없습니다");
        }
        this.status = SubscriptionStatus.EXPIRED;
        this.cancelAtPeriodEnd = false;
        clearScheduledPlanChange();
    }

    public BigDecimal calculateRemainingBillingRatio(Instant occurredAt) {
        Instant effectiveAt = requirePeriodStart(occurredAt);
        if (!currentPeriodEnd.isAfter(currentPeriodStart)) {
            throw new DomainException(CODE_SUBSCRIPTION_PERIOD_INVALID, "유효한 청구 기간이 아닙니다");
        }
        if (!effectiveAt.isBefore(currentPeriodEnd)) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        Instant ratioStart = effectiveAt.isAfter(currentPeriodStart) ? effectiveAt : currentPeriodStart;
        long totalMillis = currentPeriodEnd.toEpochMilli() - currentPeriodStart.toEpochMilli();
        long remainingMillis = currentPeriodEnd.toEpochMilli() - ratioStart.toEpochMilli();
        if (totalMillis <= 0 || remainingMillis <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(remainingMillis)
                .divide(BigDecimal.valueOf(totalMillis), 6, RoundingMode.HALF_UP);
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private WorkspacePlanType requirePlanType(WorkspacePlanType value) {
        if (value == null) {
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

    private BillingCycle requireBillingCycle(BillingCycle value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_BILLING_CYCLE_REQUIRED, "청구 주기는 필수입니다");
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

    private void validatePeriodRange(Instant periodStart, Instant periodEnd) {
        if (!periodEnd.isAfter(periodStart)) {
            throw new DomainException(CODE_SUBSCRIPTION_PERIOD_INVALID, "구독 종료 시각은 시작 시각보다 이후여야 합니다");
        }
    }

    private void validateRenewable() {
        if (status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.EXPIRED) {
            throw new DomainException(CODE_SUBSCRIPTION_INVALID_STATE, "종료된 구독은 갱신할 수 없습니다");
        }
    }

    private void validateCancelable() {
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.PAST_DUE) {
            throw new DomainException(CODE_SUBSCRIPTION_INVALID_STATE, "활성 또는 연체 구독만 예약 해지할 수 있습니다");
        }
    }
}
