package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.ChangeRequestIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChangeRequestIssueRepository extends JpaRepository<ChangeRequestIssue, UUID> {

    List<ChangeRequestIssue> findByChangeRequestId(UUID changeRequestId);

    List<ChangeRequestIssue> findByIssueId(UUID issueId);

    List<ChangeRequestIssue> findByChangeRequestIdIn(Collection<UUID> changeRequestIds);

    List<ChangeRequestIssue> findByIssueIdIn(Collection<UUID> issueIds);

    int deleteByChangeRequestIdAndIssueIdIn(UUID changeRequestId, Collection<UUID> issueIds);

    int deleteByIssueIdAndChangeRequestIdIn(UUID issueId, Collection<UUID> changeRequestIds);
}
