package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeRepository extends JpaRepository<EngineeringChange, UUID> {

    List<EngineeringChange> findAllByOrderByNumberDesc(Pageable pageable);

    List<EngineeringChange> findAllByOrderByNumberDesc();

    boolean existsByStateIn(List<EngineeringChangeState> states);
}
