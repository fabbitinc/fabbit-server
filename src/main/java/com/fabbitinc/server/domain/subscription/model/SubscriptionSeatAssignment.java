package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "subscription_seat_assignments",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subscription_seat_assignments_membership_id", columnNames = "membership_id"),
                @UniqueConstraint(name = "uq_subscription_seat_assignments_subscription_id_user_id", columnNames = {"subscription_id", "user_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionSeatAssignment extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_SEAT_ASSIGNMENT_SUBSCRIPTION_REQUIRED";
    public static final String CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_ORGANIZATION_REQUIRED = "SUBSCRIPTION_SEAT_ASSIGNMENT_ORGANIZATION_REQUIRED";
    public static final String CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_MEMBERSHIP_REQUIRED = "SUBSCRIPTION_SEAT_ASSIGNMENT_MEMBERSHIP_REQUIRED";
    public static final String CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_USER_REQUIRED = "SUBSCRIPTION_SEAT_ASSIGNMENT_USER_REQUIRED";
    public static final String CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_SEAT_TYPE_REQUIRED = "SUBSCRIPTION_SEAT_ASSIGNMENT_SEAT_TYPE_REQUIRED";
    public static final String CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_ASSIGNED_AT_REQUIRED = "SUBSCRIPTION_SEAT_ASSIGNMENT_ASSIGNED_AT_REQUIRED";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "subscription_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_seat_assignments_subscription_id")
    )
    private Subscription subscription;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "org_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_seat_assignments_org_id")
    )
    private Organization organization;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "membership_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_seat_assignments_membership_id")
    )
    private Membership membership;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_subscription_seat_assignments_user_id")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    private SubscriptionSeatAssignment(
            UUID subscriptionId,
            UUID orgId,
            UUID membershipId,
            UUID userId,
            SeatType seatType,
            UUID assignedBy,
            Instant assignedAt
    ) {
        super(UuidV7Generator.next());
        this.subscriptionId = requireRequiredId(subscriptionId, CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_SUBSCRIPTION_REQUIRED, "구독 ID는 필수입니다");
        this.orgId = requireRequiredId(orgId, CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_ORGANIZATION_REQUIRED, "조직 ID는 필수입니다");
        this.membershipId = requireRequiredId(membershipId, CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_MEMBERSHIP_REQUIRED, "멤버십 ID는 필수입니다");
        this.userId = requireRequiredId(userId, CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_USER_REQUIRED, "사용자 ID는 필수입니다");
        this.seatType = requireSeatType(seatType);
        this.assignedBy = assignedBy;
        this.assignedAt = requireAssignedAt(assignedAt);
    }

    public static SubscriptionSeatAssignment create(
            UUID subscriptionId,
            UUID orgId,
            UUID membershipId,
            UUID userId,
            SeatType seatType,
            UUID assignedBy,
            Instant assignedAt
    ) {
        return new SubscriptionSeatAssignment(subscriptionId, orgId, membershipId, userId, seatType, assignedBy, assignedAt);
    }

    public void changeSeatType(SeatType seatType, UUID assignedBy, Instant assignedAt) {
        this.seatType = requireSeatType(seatType);
        this.assignedBy = assignedBy;
        this.assignedAt = requireAssignedAt(assignedAt);
    }

    private UUID requireRequiredId(UUID value, String code, String message) {
        if (value == null) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private SeatType requireSeatType(SeatType value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_SEAT_TYPE_REQUIRED, "좌석 타입은 필수입니다");
        }
        return value;
    }

    private Instant requireAssignedAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_SUBSCRIPTION_SEAT_ASSIGNMENT_ASSIGNED_AT_REQUIRED, "좌석 배정 시각은 필수입니다");
        }
        return value;
    }
}
