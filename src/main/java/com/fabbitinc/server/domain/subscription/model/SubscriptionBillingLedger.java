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
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "subscription_billing_ledgers", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionBillingLedger extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_BILLING_LEDGER_SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_BILLING_LEDGER_SUBSCRIPTION_REQUIRED";
    public static final String CODE_SUBSCRIPTION_BILLING_LEDGER_ORG_REQUIRED = "SUBSCRIPTION_BILLING_LEDGER_ORG_REQUIRED";
    public static final String CODE_SUBSCRIPTION_BILLING_LEDGER_LEDGER_TYPE_REQUIRED = "SUBSCRIPTION_BILLING_LEDGER_LEDGER_TYPE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_BILLING_LEDGER_STATUS_REQUIRED = "SUBSCRIPTION_BILLING_LEDGER_STATUS_REQUIRED";
    public static final String CODE_SUBSCRIPTION_BILLING_LEDGER_AMOUNT_INVALID = "SUBSCRIPTION_BILLING_LEDGER_AMOUNT_INVALID";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subscription_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_billing_ledgers_subscription_id")
    )
    private Subscription subscription;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ledger_type", nullable = false, length = 30)
    private SubscriptionBillingLedgerType ledgerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionBillingLedgerStatus status;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    private SubscriptionBillingLedger(
            UUID subscriptionId,
            UUID orgId,
            SubscriptionBillingLedgerType ledgerType,
            Instant periodStart,
            Instant periodEnd,
            BigDecimal quantity,
            BigDecimal unitAmount,
            BigDecimal totalAmount,
            String currency,
            String referenceType,
            UUID referenceId,
            Map<String, Object> metadata
    ) {
        super(UuidV7Generator.next());
        this.subscriptionId = requireId(subscriptionId, CODE_SUBSCRIPTION_BILLING_LEDGER_SUBSCRIPTION_REQUIRED, "구독 ID는 필수입니다");
        this.orgId = requireId(orgId, CODE_SUBSCRIPTION_BILLING_LEDGER_ORG_REQUIRED, "조직 ID는 필수입니다");
        this.ledgerType = requireLedgerType(ledgerType);
        this.status = SubscriptionBillingLedgerStatus.PENDING;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.quantity = requireNonNegative(quantity);
        this.unitAmount = requireNonNegative(unitAmount);
        this.totalAmount = requireAmount(totalAmount);
        this.currency = currency == null ? "KRW" : currency.trim();
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static SubscriptionBillingLedger create(
            UUID subscriptionId,
            UUID orgId,
            SubscriptionBillingLedgerType ledgerType,
            Instant periodStart,
            Instant periodEnd,
            BigDecimal quantity,
            BigDecimal unitAmount,
            BigDecimal totalAmount,
            String currency,
            String referenceType,
            UUID referenceId,
            Map<String, Object> metadata
    ) {
        return new SubscriptionBillingLedger(
                subscriptionId,
                orgId,
                ledgerType,
                periodStart,
                periodEnd,
                quantity,
                unitAmount,
                totalAmount,
                currency,
                referenceType,
                referenceId,
                metadata
        );
    }

    public void markInvoiced() {
        this.status = SubscriptionBillingLedgerStatus.INVOICED;
    }

    public void markSettled() {
        this.status = SubscriptionBillingLedgerStatus.SETTLED;
    }

    public void voidLedger() {
        this.status = SubscriptionBillingLedgerStatus.VOIDED;
    }

    private UUID requireId(UUID value, String code, String message) {
        if (value == null) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private SubscriptionBillingLedgerType requireLedgerType(SubscriptionBillingLedgerType value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_BILLING_LEDGER_LEDGER_TYPE_REQUIRED, "청구 유형은 필수입니다");
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new DomainException(CODE_SUBSCRIPTION_BILLING_LEDGER_AMOUNT_INVALID, "수량과 금액은 0 이상이어야 합니다");
        }
        return value;
    }

    private BigDecimal requireAmount(BigDecimal value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_BILLING_LEDGER_AMOUNT_INVALID, "청구 금액은 필수입니다");
        }
        return value;
    }
}
