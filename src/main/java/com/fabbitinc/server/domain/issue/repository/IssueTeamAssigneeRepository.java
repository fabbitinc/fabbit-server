package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.IssueTeamAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IssueTeamAssigneeRepository extends JpaRepository<IssueTeamAssignee, UUID> {

    List<IssueTeamAssignee> findByIssueId(UUID issueId);

    List<IssueTeamAssignee> findByIssueIdIn(Collection<UUID> issueIds);

    int deleteByIssueIdAndTeamIdIn(UUID issueId, Collection<UUID> teamIds);
}
