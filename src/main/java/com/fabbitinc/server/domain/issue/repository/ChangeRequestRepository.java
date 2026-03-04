package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import com.fabbitinc.server.domain.issue.model.CrState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {

    Optional<ChangeRequest> findByNumber(int number);

    @Query("""
            select cr
            from ChangeRequest cr
            where (:state is null or cr.state = :state)
              and (:crState is null or cr.crState = :crState)
              and (:search is null or lower(cr.title) like lower(concat('%', :search, '%')))
            order by cr.createdAt desc
            """)
    List<ChangeRequest> listByFilters(
            IssueState state,
            CrState crState,
            String search,
            Pageable pageable
    );

    @Query("""
            select count(cr)
            from ChangeRequest cr
            where (:state is null or cr.state = :state)
              and (:crState is null or cr.crState = :crState)
              and (:search is null or lower(cr.title) like lower(concat('%', :search, '%')))
            """)
    long countByFilters(
            IssueState state,
            CrState crState,
            String search
    );

    List<ChangeRequest> findAllByOrderByNumberDesc(Pageable pageable);

    List<ChangeRequest> findAllByOrderByNumberDesc();
}
