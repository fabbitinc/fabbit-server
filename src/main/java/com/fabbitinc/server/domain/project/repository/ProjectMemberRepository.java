package com.fabbitinc.server.domain.project.repository;

import com.fabbitinc.server.domain.project.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByProjectIdAndUserIdIn(UUID projectId, Collection<UUID> userIds);

    int deleteByProjectIdAndUserIdIn(UUID projectId, Collection<UUID> userIds);

    long countByProjectId(UUID projectId);
}
