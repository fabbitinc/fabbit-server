package com.fabbitinc.server.domain.project.repository;

import com.fabbitinc.server.domain.project.model.Project;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndDeletedFalse(UUID id);

    Optional<Project> findByNameAndDeletedFalse(String name);

    long countByDeletedFalse();

    List<Project> findByDeletedFalseOrderByNameAsc(Pageable pageable);

    List<Project> findByDeletedFalseAndNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    long countByDeletedFalseAndNameContainingIgnoreCase(String name);

    List<Project> findByIdInAndDeletedFalseOrderByNameAsc(Collection<UUID> ids);
}
