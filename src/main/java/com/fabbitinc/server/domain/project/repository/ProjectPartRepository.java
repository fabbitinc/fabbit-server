package com.fabbitinc.server.domain.project.repository;

import com.fabbitinc.server.domain.project.model.ProjectPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProjectPartRepository extends JpaRepository<ProjectPart, UUID> {

    List<ProjectPart> findByProjectId(UUID projectId);

    List<ProjectPart> findByPartId(UUID partId);

    List<ProjectPart> findByProjectIdAndPartIdIn(UUID projectId, Collection<UUID> partIds);

    int deleteByProjectIdAndPartIdIn(UUID projectId, Collection<UUID> partIds);

    long countByProjectId(UUID projectId);

    long countByPartId(UUID partId);
}
