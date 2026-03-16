package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.EngineeringChangeReviewer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeReviewerRepository extends JpaRepository<EngineeringChangeReviewer, UUID> {

    List<EngineeringChangeReviewer> findByEngineeringChangeId(UUID engineeringChangeId);

    List<EngineeringChangeReviewer> findByEngineeringChangeIdIn(Collection<UUID> engineeringChangeIds);

    Optional<EngineeringChangeReviewer> findByEngineeringChangeIdAndUserId(UUID engineeringChangeId, UUID userId);

    int deleteByEngineeringChangeIdAndUserIdIn(UUID engineeringChangeId, Collection<UUID> userIds);
}
