package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.EngineeringChangeTeamReviewer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeTeamReviewerRepository extends JpaRepository<EngineeringChangeTeamReviewer, UUID> {

    List<EngineeringChangeTeamReviewer> findByEngineeringChangeId(UUID engineeringChangeId);

    List<EngineeringChangeTeamReviewer> findByEngineeringChangeIdIn(Collection<UUID> engineeringChangeIds);

    int deleteByEngineeringChangeIdAndTeamIdIn(UUID engineeringChangeId, Collection<UUID> teamIds);
}
