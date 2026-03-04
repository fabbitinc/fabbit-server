package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.ChangeRequestTeamReviewer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChangeRequestTeamReviewerRepository extends JpaRepository<ChangeRequestTeamReviewer, UUID> {

    List<ChangeRequestTeamReviewer> findByChangeRequestId(UUID changeRequestId);

    List<ChangeRequestTeamReviewer> findByChangeRequestIdIn(Collection<UUID> changeRequestIds);

    int deleteByChangeRequestIdAndTeamIdIn(UUID changeRequestId, Collection<UUID> teamIds);
}
