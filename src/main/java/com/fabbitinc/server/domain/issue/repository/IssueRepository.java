package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

    Optional<Issue> findByIdAndType(UUID id, IssueType type);

    Optional<Issue> findByNumberAndType(int number, IssueType type);

    @Query("""
            select coalesce(max(i.number), 0)
            from Issue i
            """)
    int findMaxNumber();

    long countByTypeAndState(IssueType type, IssueState state);

    List<Issue> findByTypeOrderByNumberDesc(IssueType type, Pageable pageable);

    List<Issue> findByTypeOrderByNumberDesc(IssueType type);
}
