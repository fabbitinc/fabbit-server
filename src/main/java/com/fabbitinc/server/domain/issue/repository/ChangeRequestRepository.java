package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {

    Optional<ChangeRequest> findByNumber(int number);

    List<ChangeRequest> findAllByOrderByNumberDesc(Pageable pageable);

    List<ChangeRequest> findAllByOrderByNumberDesc();
}
