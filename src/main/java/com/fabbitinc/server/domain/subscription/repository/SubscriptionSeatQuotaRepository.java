package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionSeatQuota;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionSeatQuotaRepository extends JpaRepository<SubscriptionSeatQuota, UUID> {

    List<SubscriptionSeatQuota> findBySubscriptionId(UUID subscriptionId);

    Optional<SubscriptionSeatQuota> findBySubscriptionIdAndSeatType(UUID subscriptionId, SeatType seatType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select quota
            from SubscriptionSeatQuota quota
            where quota.subscriptionId = ?1
              and quota.seatType = ?2
            """)
    Optional<SubscriptionSeatQuota> findBySubscriptionIdAndSeatTypeForUpdate(
            UUID subscriptionId,
            SeatType seatType
    );
}
