package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.EngineeringChange;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeRepository extends JpaRepository<EngineeringChange, UUID> {

    Optional<EngineeringChange> findByNumber(int number);

    List<EngineeringChange> findAllByOrderByNumberDesc(Pageable pageable);

    List<EngineeringChange> findAllByOrderByNumberDesc();
}
