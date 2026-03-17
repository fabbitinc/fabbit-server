package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.SubscriptionChangeRequest;
import com.fabbitinc.server.domain.subscription.model.SubscriptionChangeRequestStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionChangeRequestRepository extends JpaRepository<SubscriptionChangeRequest, UUID> {

    List<SubscriptionChangeRequest> findBySubscriptionIdAndStatus(UUID subscriptionId, SubscriptionChangeRequestStatus status);
}
