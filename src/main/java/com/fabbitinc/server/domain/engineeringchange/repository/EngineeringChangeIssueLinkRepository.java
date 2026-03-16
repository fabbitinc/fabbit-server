package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeIssueLinkRepository extends JpaRepository<EngineeringChangeIssueLink, UUID> {

    List<EngineeringChangeIssueLink> findByEngineeringChangeId(UUID engineeringChangeId);

    List<EngineeringChangeIssueLink> findByIssueId(UUID issueId);

    List<EngineeringChangeIssueLink> findByEngineeringChangeIdIn(Collection<UUID> engineeringChangeIds);

    List<EngineeringChangeIssueLink> findByIssueIdIn(Collection<UUID> issueIds);

    int deleteByEngineeringChangeIdAndIssueIdIn(UUID engineeringChangeId, Collection<UUID> issueIds);

    int deleteByIssueIdAndEngineeringChangeIdIn(UUID issueId, Collection<UUID> engineeringChangeIds);
}
