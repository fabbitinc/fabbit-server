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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "subscription_usage_policies",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subscription_usage_policies_subscription_id", columnNames = "subscription_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionUsagePolicy extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_USAGE_POLICY_SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_USAGE_POLICY_SUBSCRIPTION_REQUIRED";
    public static final String CODE_SUBSCRIPTION_USAGE_POLICY_STORAGE_INVALID = "SUBSCRIPTION_USAGE_POLICY_STORAGE_INVALID";
    public static final String CODE_SUBSCRIPTION_USAGE_POLICY_PRICE_INVALID = "SUBSCRIPTION_USAGE_POLICY_PRICE_INVALID";
    public static final String CODE_SUBSCRIPTION_USAGE_POLICY_AI_BILLING_MODE_REQUIRED = "SUBSCRIPTION_USAGE_POLICY_AI_BILLING_MODE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_USAGE_POLICY_AI_LIMIT_INVALID = "SUBSCRIPTION_USAGE_POLICY_AI_LIMIT_INVALID";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subscription_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_usage_policies_subscription_id")
    )
    private Subscription subscription;

    @Column(name = "base_storage_bytes", nullable = false)
    private long baseStorageBytes;

    @Column(name = "extra_storage_bytes_per_full_seat", nullable = false)
    private long extraStorageBytesPerFullSeat;

    @Column(name = "storage_overage_unit_bytes", nullable = false)
    private long storageOverageUnitBytes;

    @Column(name = "storage_overage_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal storageOverageUnitPrice;

    @Column(name = "starter_monthly_ai_credits", nullable = false, precision = 12, scale = 4)
    private BigDecimal starterMonthlyAiCredits;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_billing_mode", nullable = false, length = 20)
    private AiBillingMode aiBillingMode;

    @Column(name = "ai_monthly_credit_limit", precision = 12, scale = 4)
    private BigDecimal aiMonthlyCreditLimit;

    @Column(name = "ai_hard_limit_enabled", nullable = false)
    private boolean aiHardLimitEnabled;

    private SubscriptionUsagePolicy(
            UUID subscriptionId,
            long baseStorageBytes,
            long extraStorageBytesPerFullSeat,
            long storageOverageUnitBytes,
            BigDecimal storageOverageUnitPrice,
            BigDecimal starterMonthlyAiCredits,
            AiBillingMode aiBillingMode,
            BigDecimal aiMonthlyCreditLimit,
            boolean aiHardLimitEnabled
    ) {
        super(UuidV7Generator.next());
        this.subscriptionId = requireSubscriptionId(subscriptionId);
        this.baseStorageBytes = requireNonNegativeStorage(baseStorageBytes);
        this.extraStorageBytesPerFullSeat = requireNonNegativeStorage(extraStorageBytesPerFullSeat);
        this.storageOverageUnitBytes = requirePositiveStorage(storageOverageUnitBytes);
        this.storageOverageUnitPrice = requireNonNegativePrice(storageOverageUnitPrice);
        this.starterMonthlyAiCredits = requireNonNegativePrice(starterMonthlyAiCredits);
        this.aiBillingMode = requireAiBillingMode(aiBillingMode);
        this.aiMonthlyCreditLimit = normalizeNonNegative(aiMonthlyCreditLimit);
        this.aiHardLimitEnabled = aiHardLimitEnabled;
    }

    public static SubscriptionUsagePolicy create(
            UUID subscriptionId,
            long baseStorageBytes,
            long extraStorageBytesPerFullSeat,
            long storageOverageUnitBytes,
            BigDecimal storageOverageUnitPrice,
            BigDecimal starterMonthlyAiCredits,
            AiBillingMode aiBillingMode,
            BigDecimal aiMonthlyCreditLimit,
            boolean aiHardLimitEnabled
    ) {
        return new SubscriptionUsagePolicy(
                subscriptionId,
                baseStorageBytes,
                extraStorageBytesPerFullSeat,
                storageOverageUnitBytes,
                storageOverageUnitPrice,
                starterMonthlyAiCredits,
                aiBillingMode,
                aiMonthlyCreditLimit,
                aiHardLimitEnabled
        );
    }

    public long calculateIncludedStorageBytes(long fullSeatCount) {
        return baseStorageBytes + (Math.max(fullSeatCount, 0L) * extraStorageBytesPerFullSeat);
    }

    public void changeAiLimit(BigDecimal aiMonthlyCreditLimit, boolean aiHardLimitEnabled) {
        this.aiMonthlyCreditLimit = normalizeNonNegative(aiMonthlyCreditLimit);
        this.aiHardLimitEnabled = aiHardLimitEnabled;
    }

    public void applyPlanDefaults(WorkspacePlanType planType) {
        this.baseStorageBytes = requireNonNegativeStorage(planType.baseStorageBytes());
        this.extraStorageBytesPerFullSeat = requireNonNegativeStorage(planType.extraStorageBytesPerFullSeat());
        this.storageOverageUnitBytes = requirePositiveStorage(WorkspacePlanType.STORAGE_OVERAGE_UNIT_BYTES);
        this.storageOverageUnitPrice = requireNonNegativePrice(WorkspacePlanType.STORAGE_OVERAGE_UNIT_PRICE);
        this.starterMonthlyAiCredits = requireNonNegativePrice(BigDecimal.valueOf(planType.starterMonthlyAiCredits()));
        this.aiBillingMode = requireAiBillingMode(planType.aiBillingMode());
        if (planType.isStarter()) {
            this.aiMonthlyCreditLimit = null;
            this.aiHardLimitEnabled = true;
            return;
        }
        if (this.aiMonthlyCreditLimit == null) {
            this.aiHardLimitEnabled = false;
        }
    }

    private UUID requireSubscriptionId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_USAGE_POLICY_SUBSCRIPTION_REQUIRED, "구독 ID는 필수입니다");
        }
        return value;
    }

    private long requireNonNegativeStorage(long value) {
        if (value < 0) {
            throw new DomainException(CODE_SUBSCRIPTION_USAGE_POLICY_STORAGE_INVALID, "스토리지 값은 0 이상이어야 합니다");
        }
        return value;
    }

    private long requirePositiveStorage(long value) {
        if (value <= 0) {
            throw new DomainException(CODE_SUBSCRIPTION_USAGE_POLICY_STORAGE_INVALID, "스토리지 단위는 1 이상이어야 합니다");
        }
        return value;
    }

    private BigDecimal requireNonNegativePrice(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new DomainException(CODE_SUBSCRIPTION_USAGE_POLICY_PRICE_INVALID, "가격 또는 크레딧 값은 0 이상이어야 합니다");
        }
        return value;
    }

    private AiBillingMode requireAiBillingMode(AiBillingMode value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_USAGE_POLICY_AI_BILLING_MODE_REQUIRED, "AI 과금 방식은 필수입니다");
        }
        return value;
    }

    private BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0) {
            throw new DomainException(CODE_SUBSCRIPTION_USAGE_POLICY_AI_LIMIT_INVALID, "AI 한도는 0 이상이어야 합니다");
        }
        return value;
    }
}
