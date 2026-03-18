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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "subscription_credit_purchases", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionCreditPurchase extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_CREDIT_PURCHASE_SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_CREDIT_PURCHASE_SUBSCRIPTION_REQUIRED";
    public static final String CODE_SUBSCRIPTION_CREDIT_PURCHASE_ORG_REQUIRED = "SUBSCRIPTION_CREDIT_PURCHASE_ORG_REQUIRED";
    public static final String CODE_SUBSCRIPTION_CREDIT_PURCHASE_CREDITS_INVALID = "SUBSCRIPTION_CREDIT_PURCHASE_CREDITS_INVALID";
    public static final String CODE_SUBSCRIPTION_CREDIT_PURCHASE_PRICE_INVALID = "SUBSCRIPTION_CREDIT_PURCHASE_PRICE_INVALID";
    public static final String CODE_SUBSCRIPTION_CREDIT_PURCHASE_STATUS_REQUIRED = "SUBSCRIPTION_CREDIT_PURCHASE_STATUS_REQUIRED";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subscription_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_credit_purchases_subscription_id")
    )
    private Subscription subscription;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionCreditPurchaseStatus status;

    @Column(name = "credits_purchased", nullable = false, precision = 12, scale = 4)
    private BigDecimal creditsPurchased;

    @Column(name = "credits_remaining", nullable = false, precision = 12, scale = 4)
    private BigDecimal creditsRemaining;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "expires_at")
    private Instant expiresAt;

    private SubscriptionCreditPurchase(
            UUID subscriptionId,
            UUID orgId,
            BigDecimal creditsPurchased,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String currency,
            Instant expiresAt
    ) {
        super(UuidV7Generator.next());
        this.subscriptionId = requireId(subscriptionId, CODE_SUBSCRIPTION_CREDIT_PURCHASE_SUBSCRIPTION_REQUIRED, "구독 ID는 필수입니다");
        this.orgId = requireId(orgId, CODE_SUBSCRIPTION_CREDIT_PURCHASE_ORG_REQUIRED, "조직 ID는 필수입니다");
        this.creditsPurchased = requirePositive(creditsPurchased, CODE_SUBSCRIPTION_CREDIT_PURCHASE_CREDITS_INVALID, "구매 크레딧은 0보다 커야 합니다");
        this.creditsRemaining = this.creditsPurchased;
        this.unitPrice = requireNonNegative(unitPrice, CODE_SUBSCRIPTION_CREDIT_PURCHASE_PRICE_INVALID, "단가는 0 이상이어야 합니다");
        this.totalAmount = requireNonNegative(totalAmount, CODE_SUBSCRIPTION_CREDIT_PURCHASE_PRICE_INVALID, "총액은 0 이상이어야 합니다");
        this.currency = requireCurrency(currency);
        this.expiresAt = expiresAt;
        this.status = SubscriptionCreditPurchaseStatus.ACTIVE;
    }

    public static SubscriptionCreditPurchase create(
            UUID subscriptionId,
            UUID orgId,
            BigDecimal creditsPurchased,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String currency,
            Instant expiresAt
    ) {
        return new SubscriptionCreditPurchase(subscriptionId, orgId, creditsPurchased, unitPrice, totalAmount, currency, expiresAt);
    }

    public void consume(BigDecimal credits) {
        BigDecimal amount = requirePositive(credits, CODE_SUBSCRIPTION_CREDIT_PURCHASE_CREDITS_INVALID, "차감 크레딧은 0보다 커야 합니다");
        this.creditsRemaining = this.creditsRemaining.subtract(amount).max(BigDecimal.ZERO);
        if (this.creditsRemaining.signum() == 0) {
            this.status = SubscriptionCreditPurchaseStatus.EXHAUSTED;
        }
    }

    public void expire() {
        this.status = SubscriptionCreditPurchaseStatus.EXPIRED;
    }

    private UUID requireId(UUID value, String code, String message) {
        if (value == null) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private BigDecimal requirePositive(BigDecimal value, String code, String message) {
        if (value == null || value.signum() <= 0) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String code, String message) {
        if (value == null || value.signum() < 0) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private String requireCurrency(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_SUBSCRIPTION_CREDIT_PURCHASE_STATUS_REQUIRED, "통화는 필수입니다");
        }
        return value.trim();
    }
}
