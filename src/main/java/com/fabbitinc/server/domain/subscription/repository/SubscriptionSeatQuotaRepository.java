package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionSeatQuota;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionSeatQuotaRepository extends JpaRepository<SubscriptionSeatQuota, UUID> {

    List<SubscriptionSeatQuota> findBySubscriptionId(UUID subscriptionId);

    Optional<SubscriptionSeatQuota> findBySubscriptionIdAndSeatType(UUID subscriptionId, SeatType seatType);
}
