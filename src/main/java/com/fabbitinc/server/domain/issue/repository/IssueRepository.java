package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.model.IssueState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

    long countByState(IssueState state);

    Optional<Issue> findTopByOrderByNumberDesc();

    List<Issue> findAllByOrderByNumberDesc(Pageable pageable);

    List<Issue> findAllByOrderByNumberDesc();
}
