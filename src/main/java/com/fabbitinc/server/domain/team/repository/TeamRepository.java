package com.fabbitinc.server.domain.team.repository;

import com.fabbitinc.server.domain.team.model.Team;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findAllByOrderByNameAsc();

    List<Team> findAllByOrderByNameAsc(Pageable pageable);

    List<Team> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
