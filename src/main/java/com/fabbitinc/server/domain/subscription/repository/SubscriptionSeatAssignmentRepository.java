package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionSeatAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionSeatAssignmentRepository extends JpaRepository<SubscriptionSeatAssignment, UUID> {

    long countByOrgId(UUID orgId);

    long countByOrgIdAndSeatType(UUID orgId, SeatType seatType);

    List<SubscriptionSeatAssignment> findByOrgId(UUID orgId);

    List<SubscriptionSeatAssignment> findBySubscriptionId(UUID subscriptionId);

    Optional<SubscriptionSeatAssignment> findByOrgIdAndUserId(UUID orgId, UUID userId);

    Optional<SubscriptionSeatAssignment> findByMembershipId(UUID membershipId);
}
