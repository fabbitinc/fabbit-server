package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.ChangeRequestReviewer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRequestReviewerRepository extends JpaRepository<ChangeRequestReviewer, UUID> {

    List<ChangeRequestReviewer> findByChangeRequestId(UUID changeRequestId);

    List<ChangeRequestReviewer> findByChangeRequestIdIn(Collection<UUID> changeRequestIds);

    Optional<ChangeRequestReviewer> findByChangeRequestIdAndUserId(UUID changeRequestId, UUID userId);

    int deleteByChangeRequestIdAndUserIdIn(UUID changeRequestId, Collection<UUID> userIds);
}
