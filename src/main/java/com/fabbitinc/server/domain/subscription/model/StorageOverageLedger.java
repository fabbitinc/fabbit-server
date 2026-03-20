package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "storage_overage_ledgers", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageOverageLedger extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_STORAGE_OVERAGE_LEDGER_SUBSCRIPTION_REQUIRED = "STORAGE_OVERAGE_LEDGER_SUBSCRIPTION_REQUIRED";
    public static final String CODE_STORAGE_OVERAGE_LEDGER_ORGANIZATION_REQUIRED = "STORAGE_OVERAGE_LEDGER_ORGANIZATION_REQUIRED";
    public static final String CODE_STORAGE_OVERAGE_LEDGER_PERIOD_REQUIRED = "STORAGE_OVERAGE_LEDGER_PERIOD_REQUIRED";
    public static final String CODE_STORAGE_OVERAGE_LEDGER_AMOUNT_INVALID = "STORAGE_OVERAGE_LEDGER_AMOUNT_INVALID";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subscription_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_storage_overage_ledgers_subscription_id")
    )
    private Subscription subscription;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "overage_bytes", nullable = false)
    private long overageBytes;

    @Column(name = "billable_gb", nullable = false, precision = 19, scale = 6)
    private BigDecimal billableGb;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionBillingLedgerStatus status;

    private StorageOverageLedger(
            UUID subscriptionId,
            UUID orgId,
            UUID snapshotId,
            Instant periodStart,
            Instant periodEnd,
            long overageBytes,
            BigDecimal billableGb,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String currency
    ) {
        super(UuidV7Generator.next());
        this.subscriptionId = requireId(subscriptionId, CODE_STORAGE_OVERAGE_LEDGER_SUBSCRIPTION_REQUIRED, "구독 ID는 필수입니다");
        this.orgId = requireId(orgId, CODE_STORAGE_OVERAGE_LEDGER_ORGANIZATION_REQUIRED, "조직 ID는 필수입니다");
        this.snapshotId = snapshotId;
        this.periodStart = requireInstant(periodStart);
        this.periodEnd = requireInstant(periodEnd);
        this.overageBytes = requireNonNegative(overageBytes);
        this.billableGb = requireNonNegative(billableGb);
        this.unitPrice = requireNonNegative(unitPrice);
        this.totalAmount = requireNonNegative(totalAmount);
        this.currency = currency == null ? "KRW" : currency.trim();
        this.status = SubscriptionBillingLedgerStatus.PENDING;
    }

    public static StorageOverageLedger create(
            UUID subscriptionId,
            UUID orgId,
            UUID snapshotId,
            Instant periodStart,
            Instant periodEnd,
            long overageBytes,
            BigDecimal billableGb,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String currency
    ) {
        return new StorageOverageLedger(
                subscriptionId,
                orgId,
                snapshotId,
                periodStart,
                periodEnd,
                overageBytes,
                billableGb,
                unitPrice,
                totalAmount,
                currency
        );
    }

    public void markInvoiced() {
        this.status = SubscriptionBillingLedgerStatus.INVOICED;
    }

    public void markSettled() {
        this.status = SubscriptionBillingLedgerStatus.SETTLED;
    }

    private UUID requireId(UUID value, String code, String message) {
        if (value == null) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private Instant requireInstant(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_STORAGE_OVERAGE_LEDGER_PERIOD_REQUIRED, "정산 기간은 필수입니다");
        }
        return value;
    }

    private long requireNonNegative(long value) {
        if (value < 0) {
            throw new DomainException(CODE_STORAGE_OVERAGE_LEDGER_AMOUNT_INVALID, "바이트는 0 이상이어야 합니다");
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new DomainException(CODE_STORAGE_OVERAGE_LEDGER_AMOUNT_INVALID, "수량과 금액은 0 이상이어야 합니다");
        }
        return value;
    }
}
