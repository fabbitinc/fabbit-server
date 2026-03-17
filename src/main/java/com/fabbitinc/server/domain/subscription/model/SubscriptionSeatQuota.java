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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "subscription_seat_quotas",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_subscription_seat_quotas_subscription_id_seat_type",
                        columnNames = {"subscription_id", "seat_type"}
                )
        },
        indexes = {
                @Index(name = "ix_subscription_seat_quotas_subscription_id", columnList = "subscription_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionSeatQuota extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_SEAT_QUOTA_SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_SEAT_QUOTA_SUBSCRIPTION_REQUIRED";
    public static final String CODE_SUBSCRIPTION_SEAT_QUOTA_SEAT_TYPE_REQUIRED = "SUBSCRIPTION_SEAT_QUOTA_SEAT_TYPE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_SEAT_QUOTA_PURCHASED_QUANTITY_INVALID = "SUBSCRIPTION_SEAT_QUOTA_PURCHASED_QUANTITY_INVALID";
    public static final String CODE_SUBSCRIPTION_SEAT_QUOTA_UNIT_PRICE_INVALID = "SUBSCRIPTION_SEAT_QUOTA_UNIT_PRICE_INVALID";
    public static final String CODE_SUBSCRIPTION_SEAT_QUOTA_CURRENCY_REQUIRED = "SUBSCRIPTION_SEAT_QUOTA_CURRENCY_REQUIRED";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subscription_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_seat_quotas_subscription_id")
    )
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Column(name = "purchased_quantity", nullable = false)
    private int purchasedQuantity;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    private SubscriptionSeatQuota(
            UUID subscriptionId,
            SeatType seatType,
            int purchasedQuantity,
            int unitPrice,
            String currency
    ) {
        super(UuidV7Generator.next());
        this.subscriptionId = requireSubscriptionId(subscriptionId);
        this.seatType = requireSeatType(seatType);
        this.purchasedQuantity = requireNonNegative(purchasedQuantity, CODE_SUBSCRIPTION_SEAT_QUOTA_PURCHASED_QUANTITY_INVALID, "구매 좌석 수는 0 이상이어야 합니다");
        this.unitPrice = requireNonNegative(unitPrice, CODE_SUBSCRIPTION_SEAT_QUOTA_UNIT_PRICE_INVALID, "좌석 단가는 0 이상이어야 합니다");
        this.currency = requireCurrency(currency);
    }

    public static SubscriptionSeatQuota create(
            UUID subscriptionId,
            SeatType seatType,
            int purchasedQuantity,
            int unitPrice,
            String currency
    ) {
        return new SubscriptionSeatQuota(subscriptionId, seatType, purchasedQuantity, unitPrice, currency);
    }

    public void increasePurchasedQuantity(int quantity) {
        this.purchasedQuantity += requireNonNegative(quantity, CODE_SUBSCRIPTION_SEAT_QUOTA_PURCHASED_QUANTITY_INVALID, "증가 좌석 수는 0 이상이어야 합니다");
    }

    public void decreasePurchasedQuantity(int quantity) {
        int delta = requireNonNegative(quantity, CODE_SUBSCRIPTION_SEAT_QUOTA_PURCHASED_QUANTITY_INVALID, "감소 좌석 수는 0 이상이어야 합니다");
        this.purchasedQuantity = Math.max(0, this.purchasedQuantity - delta);
    }

    private UUID requireSubscriptionId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_SEAT_QUOTA_SUBSCRIPTION_REQUIRED, "구독 ID는 필수입니다");
        }
        return value;
    }

    private SeatType requireSeatType(SeatType value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_SEAT_QUOTA_SEAT_TYPE_REQUIRED, "좌석 타입은 필수입니다");
        }
        return value;
    }

    private int requireNonNegative(int value, String code, String message) {
        if (value < 0) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private String requireCurrency(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_SUBSCRIPTION_SEAT_QUOTA_CURRENCY_REQUIRED, "통화는 필수입니다");
        }
        return value.trim();
    }
}
