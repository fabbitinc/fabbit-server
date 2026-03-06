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
public class Subscription extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_ORG_REQUIRED = "SUBSCRIPTION_ORG_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PLAN_TYPE_REQUIRED = "SUBSCRIPTION_PLAN_TYPE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PLAN_TYPE_TOO_LONG = "SUBSCRIPTION_PLAN_TYPE_TOO_LONG";
    public static final String CODE_SUBSCRIPTION_STATUS_REQUIRED = "SUBSCRIPTION_STATUS_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_START_REQUIRED = "SUBSCRIPTION_PERIOD_START_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_END_REQUIRED = "SUBSCRIPTION_PERIOD_END_REQUIRED";
    public static final String CODE_SUBSCRIPTION_PERIOD_INVALID = "SUBSCRIPTION_PERIOD_INVALID";
    public static final String CODE_SUBSCRIPTION_MAX_MEMBERS_INVALID = "SUBSCRIPTION_MAX_MEMBERS_INVALID";
    public static final String CODE_SUBSCRIPTION_AI_CREDITS_INVALID = "SUBSCRIPTION_AI_CREDITS_INVALID";
    public static final String CODE_SUBSCRIPTION_STORAGE_LIMIT_INVALID = "SUBSCRIPTION_STORAGE_LIMIT_INVALID";
    public static final String CODE_SUBSCRIPTION_INVALID_STATE = "SUBSCRIPTION_INVALID_STATE";

    private static final int MAX_PLAN_TYPE_LENGTH = 20;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "org_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscriptions_org_id")
    )
    private Organization _organizationRelation;

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

    private Subscription(
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
        validatePeriodRange(this.currentPeriodStart, this.currentPeriodEnd);
        this.maxMembers = requireMaxMembers(maxMembers);
        this.aiCreditsGranted = requireNonNegative(aiCreditsGranted, CODE_SUBSCRIPTION_AI_CREDITS_INVALID, "AI 크레딧은 0 이상이어야 합니다");
        this.storageBytesLimit = requireNonNegative(storageBytesLimit, CODE_SUBSCRIPTION_STORAGE_LIMIT_INVALID, "스토리지 한도는 0 이상이어야 합니다");
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

    public void changePlan(String planType, int maxMembers, int aiCreditsGranted, long storageBytesLimit) {
        this.planType = requirePlanType(planType);
        this.maxMembers = requireMaxMembers(maxMembers);
        this.aiCreditsGranted = requireNonNegative(aiCreditsGranted, CODE_SUBSCRIPTION_AI_CREDITS_INVALID, "AI 크레딧은 0 이상이어야 합니다");
        this.storageBytesLimit = requireNonNegative(storageBytesLimit, CODE_SUBSCRIPTION_STORAGE_LIMIT_INVALID, "스토리지 한도는 0 이상이어야 합니다");
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
        String trimmed = value.trim();
        if (trimmed.length() > MAX_PLAN_TYPE_LENGTH) {
            throw new DomainException(CODE_SUBSCRIPTION_PLAN_TYPE_TOO_LONG, "플랜 타입은 20자 이하여야 합니다");
        }
        return trimmed;
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

    private int requireMaxMembers(int value) {
        if (value == 0 || value < -1) {
            throw new DomainException(CODE_SUBSCRIPTION_MAX_MEMBERS_INVALID, "최대 멤버 수는 -1(무제한) 또는 1 이상이어야 합니다");
        }
        return value;
    }

    private int requireNonNegative(int value, String code, String message) {
        if (value < 0) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private long requireNonNegative(long value, String code, String message) {
        if (value < 0L) {
            throw new DomainException(code, message);
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
            throw new DomainException(CODE_SUBSCRIPTION_INVALID_STATE, "해지되었거나 만료된 구독은 갱신할 수 없습니다");
        }
    }

    private void validateCancelable() {
        if (status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.EXPIRED) {
            throw new DomainException(CODE_SUBSCRIPTION_INVALID_STATE, "해지되었거나 만료된 구독은 정기 해지 설정을 변경할 수 없습니다");
        }
    }
}
