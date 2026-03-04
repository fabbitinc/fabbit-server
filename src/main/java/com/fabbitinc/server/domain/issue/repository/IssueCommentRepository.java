package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {

    List<IssueComment> findByIssueIdOrderByCreatedAtAsc(UUID issueId);

    long countByIssueId(UUID issueId);

    @Query("""
            select c.issueId, count(c.id)
            from IssueComment c
            where c.issueId in :issueIds
            group by c.issueId
            """)
    List<Object[]> countByIssueIds(Collection<UUID> issueIds);
}
