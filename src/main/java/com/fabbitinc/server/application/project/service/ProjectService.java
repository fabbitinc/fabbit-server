package com.fabbitinc.server.application.project.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.project.model.Project;
import com.fabbitinc.server.domain.project.model.ProjectMember;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.model.ProjectRole;
import com.fabbitinc.server.domain.project.repository.ProjectMemberRepository;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPartRepository projectPartRepository;
    private final PartApi partApi;

    public Project createProject(UUID ownerId, String name, String description) {
        try {
            Project project = Project.create(name, description);
            ProjectMember ownerMember = project.addMember(ownerId, ProjectRole.ADMIN);
            projectRepository.save(project);
            projectMemberRepository.save(ownerMember);
            return project;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public Project updateProject(UUID projectId, String name, String description) {
        Project project = getOrThrow(projectId);
        try {
            if (name != null && !name.equals(project.getName())) {
                project.rename(name);
            }
            if (description != null && !description.equals(project.getDescription())) {
                project.changeDescription(description);
            }
            return project;
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public void archiveProject(UUID projectId, UUID userId) {
        ensureProjectAdmin(projectId, userId);
        Project project = getOrThrow(projectId);
        try {
            project.archive();
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public void unarchiveProject(UUID projectId, UUID userId) {
        ensureProjectAdmin(projectId, userId);
        Project project = getOrThrow(projectId);
        try {
            project.unarchive();
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public void deleteProject(UUID projectId, UUID userId) {
        ensureProjectAdmin(projectId, userId);
        Project project = getOrThrow(projectId);
        project.softDelete(userId.toString());
    }

    public int linkParts(UUID projectId, List<UUID> partIds) {
        Project project = getOrThrow(projectId);
        try {
            project.ensureActive();
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
        validateIds(partIds, "part_ids");

        for (UUID partId : partIds) {
            if (!partApi.existsPart(partId)) {
                throw new AppException(ErrorCode.NOT_FOUND, "Part '" + partId + "'을(를) 찾을 수 없습니다");
            }
        }

        List<UUID> normalizedPartIds = List.copyOf(new LinkedHashSet<>(partIds));
        Set<UUID> existingPartIds = projectPartRepository.findByProjectIdAndPartIdIn(projectId, normalizedPartIds).stream()
                .map(ProjectPart::getPartId)
                .collect(java.util.stream.Collectors.toSet());

        try {
            List<ProjectPart> newLinks = normalizedPartIds.stream()
                    .filter(partId -> !existingPartIds.contains(partId))
                    .map(project::linkPart)
                    .toList();
            if (newLinks.isEmpty()) {
                return 0;
            }

            projectPartRepository.saveAll(newLinks);
            return newLinks.size();
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public int unlinkParts(UUID projectId, List<UUID> partIds) {
        getOrThrow(projectId);
        validateIds(partIds, "part_ids");
        List<UUID> normalizedPartIds = List.copyOf(new LinkedHashSet<>(partIds));
        return projectPartRepository.deleteByProjectIdAndPartIdIn(projectId, normalizedPartIds);
    }

    public int addMembers(UUID projectId, List<UUID> userIds, ProjectRole role) {
        Project project = getOrThrow(projectId);
        if (role == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 역할입니다");
        }
        validateIds(userIds, "user_ids");

        List<UUID> normalizedUserIds = List.copyOf(new LinkedHashSet<>(userIds));
        Set<UUID> existingUserIds = projectMemberRepository.findByProjectIdAndUserIdIn(projectId, normalizedUserIds).stream()
                .map(ProjectMember::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        try {
            List<ProjectMember> newMembers = normalizedUserIds.stream()
                    .filter(userId -> !existingUserIds.contains(userId))
                    .map(userId -> project.addMember(userId, role))
                    .toList();
            if (newMembers.isEmpty()) {
                return 0;
            }

            projectMemberRepository.saveAll(newMembers);
            return newMembers.size();
        } catch (DomainException ex) {
            throw toAppException(ex);
        }
    }

    public int removeMembers(UUID projectId, List<UUID> userIds) {
        getOrThrow(projectId);
        validateIds(userIds, "user_ids");
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

    public void ensureProjectAdmin(UUID projectId, UUID userId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "프로젝트 관리자 권한이 필요합니다"));
        if (!member.isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN, "프로젝트 관리자 권한이 필요합니다");
        }
    }

    private AppException toAppException(DomainException ex) {
        return switch (ex.getDomainCode()) {
            case Project.CODE_PROJECT_ARCHIVED ->
                    new AppException(ErrorCode.PROJECT_ARCHIVED, ex.getMessage());
            case Project.CODE_PROJECT_ALREADY_ARCHIVED, Project.CODE_PROJECT_NOT_ARCHIVED ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
            case Project.CODE_PROJECT_NAME_REQUIRED,
                    Project.CODE_PROJECT_NAME_TOO_LONG,
                    ProjectMember.CODE_PROJECT_MEMBER_PROJECT_REQUIRED,
                    ProjectMember.CODE_PROJECT_MEMBER_USER_REQUIRED,
                    ProjectMember.CODE_PROJECT_MEMBER_ROLE_REQUIRED,
                    ProjectPart.CODE_PROJECT_PART_PROJECT_REQUIRED,
                    ProjectPart.CODE_PROJECT_PART_PART_REQUIRED ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }

    private void validateIds(List<UUID> ids, String field) {
        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, field + "는 1개 이상이어야 합니다");
        }
        if (ids.stream().anyMatch(java.util.Objects::isNull)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, field + "에 null 값을 포함할 수 없습니다");
        }
    }
}
