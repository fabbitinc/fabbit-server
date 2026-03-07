package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.IssueAssignee;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueAssigneeRepository extends JpaRepository<IssueAssignee, UUID> {

    List<IssueAssignee> findByIssueId(UUID issueId);

    List<IssueAssignee> findByIssueIdIn(Collection<UUID> issueIds);

    int deleteByIssueIdAndUserIdIn(UUID issueId, Collection<UUID> userIds);
}
