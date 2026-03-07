package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.IssuePart;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuePartRepository extends JpaRepository<IssuePart, UUID> {

    List<IssuePart> findByIssueId(UUID issueId);

    List<IssuePart> findByIssueIdIn(Collection<UUID> issueIds);

    int deleteByIssueIdAndPartIdIn(UUID issueId, Collection<UUID> partIds);
}
