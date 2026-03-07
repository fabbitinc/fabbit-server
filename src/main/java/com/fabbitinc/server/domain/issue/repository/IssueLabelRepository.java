package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.IssueLabel;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueLabelRepository extends JpaRepository<IssueLabel, UUID> {

    List<IssueLabel> findByIssueId(UUID issueId);

    List<IssueLabel> findByIssueIdIn(Collection<UUID> issueIds);

    int deleteByIssueIdAndLabelIdIn(UUID issueId, Collection<UUID> labelIds);
}
