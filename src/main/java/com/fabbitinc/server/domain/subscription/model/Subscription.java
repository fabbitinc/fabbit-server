package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
        indexes = {
                @Index(name = "ix_subscriptions_org_id", columnList = "org_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends AbstractAuditableEntity {

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

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
        this.orgId = orgId;
        this.planType = planType;
        this.status = status;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.maxMembers = maxMembers;
        this.aiCreditsGranted = aiCreditsGranted;
        this.storageBytesLimit = storageBytesLimit;
        this.cancelAtPeriodEnd = false;
    }
}
