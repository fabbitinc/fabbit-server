package com.fabbitinc.server.application.project.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.project.model.Project;
import com.fabbitinc.server.domain.project.model.ProjectMember;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.model.ProjectRole;
import com.fabbitinc.server.domain.project.repository.ProjectMemberRepository;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPartRepository projectPartRepository;
    private final PartRepository partRepository;

    public Project createProject(UUID ownerId, String name, String description) {
        Project project = projectRepository.save(new Project(name, description));
        projectMemberRepository.save(new ProjectMember(project.getId(), ownerId, ProjectRole.ADMIN));
        return project;
    }

    public Project updateProject(UUID projectId, String name, String description) {
        Project project = getOrThrow(projectId);
        ensureProjectActive(project);

        if (name != null && !name.equals(project.getName())) {
            project.rename(name);
        }
        if (description != null && !description.equals(project.getDescription())) {
            project.updateDescription(description);
        }
        return project;
    }

    public void archiveProject(UUID projectId, UUID userId) {
        ensureProjectAdmin(projectId, userId);
        Project project = getOrThrow(projectId);
        ensureProjectActive(project);
        project.archive();
    }

    public void unarchiveProject(UUID projectId, UUID userId) {
        ensureProjectAdmin(projectId, userId);
        Project project = getOrThrow(projectId);
        if (!project.isArchived()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "보관 상태가 아닌 프로젝트는 복원할 수 없습니다");
        }
        project.unarchive();
    }

    public void deleteProject(UUID projectId, UUID userId) {
        ensureProjectAdmin(projectId, userId);
        Project project = getOrThrow(projectId);
        project.softDelete(userId.toString());
    }

    public int linkParts(UUID projectId, List<UUID> partIds) {
        Project project = getOrThrow(projectId);
        ensureProjectActive(project);

        for (UUID partId : partIds) {
            if (partRepository.findById(partId).isEmpty()) {
                throw new AppException(ErrorCode.NOT_FOUND, "Part '" + partId + "'을(를) 찾을 수 없습니다");
            }
        }

        List<UUID> normalizedPartIds = List.copyOf(new LinkedHashSet<>(partIds));
        Set<UUID> existingPartIds = projectPartRepository.findByProjectIdAndPartIdIn(projectId, normalizedPartIds).stream()
                .map(ProjectPart::getPartId)
                .collect(java.util.stream.Collectors.toSet());

        List<ProjectPart> newLinks = normalizedPartIds.stream()
                .filter(partId -> !existingPartIds.contains(partId))
                .map(partId -> new ProjectPart(projectId, partId))
                .toList();
        if (newLinks.isEmpty()) {
            return 0;
        }

        projectPartRepository.saveAll(newLinks);
        return newLinks.size();
    }

    public int unlinkParts(UUID projectId, List<UUID> partIds) {
        getOrThrow(projectId);
        List<UUID> normalizedPartIds = List.copyOf(new LinkedHashSet<>(partIds));
        return projectPartRepository.deleteByProjectIdAndPartIdIn(projectId, normalizedPartIds);
    }

    public int addMembers(UUID projectId, List<UUID> userIds, ProjectRole role) {
        getOrThrow(projectId);

        List<UUID> normalizedUserIds = List.copyOf(new LinkedHashSet<>(userIds));
        Set<UUID> existingUserIds = projectMemberRepository.findByProjectIdAndUserIdIn(projectId, normalizedUserIds).stream()
                .map(ProjectMember::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        List<ProjectMember> newMembers = normalizedUserIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .map(userId -> new ProjectMember(projectId, userId, role))
                .toList();
        if (newMembers.isEmpty()) {
            return 0;
        }

        projectMemberRepository.saveAll(newMembers);
        return newMembers.size();
    }

    public int removeMembers(UUID projectId, List<UUID> userIds) {
        getOrThrow(projectId);
        List<UUID> normalizedUserIds = List.copyOf(new LinkedHashSet<>(userIds));
        return projectMemberRepository.deleteByProjectIdAndUserIdIn(projectId, normalizedUserIds);
    }

    public Project getOrThrow(UUID projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Project '" + projectId + "'을(를) 찾을 수 없습니다"
                ));
    }

    public void ensureProjectActive(Project project) {
        if (project.isArchived()) {
            throw new AppException(ErrorCode.PROJECT_ARCHIVED, "보관된 프로젝트는 수정할 수 없습니다");
        }
    }

    public void ensureProjectAdmin(UUID projectId, UUID userId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "프로젝트 관리자 권한이 필요합니다"));
        if (member.getRole() != ProjectRole.ADMIN) {
            throw new AppException(ErrorCode.FORBIDDEN, "프로젝트 관리자 권한이 필요합니다");
        }
    }
}
