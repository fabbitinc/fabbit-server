package com.fabbitinc.server.application.project.query;

import com.fabbitinc.server.application.activity.dto.response.ActivityListResponse;
import com.fabbitinc.server.application.activity.dto.response.ActivityResponse;
import com.fabbitinc.server.application.activity.dto.response.UserSummaryResponse;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.part.dto.response.PartLookupItemResponse;
import com.fabbitinc.server.application.part.dto.response.PartLookupResponse;
import com.fabbitinc.server.application.project.dto.response.MemberLookupResponse;
import com.fabbitinc.server.application.project.dto.response.PartProjectSummaryResponse;
import com.fabbitinc.server.application.project.dto.response.PartProjectsResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectDetailResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectListResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectMemberListResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectMemberSummaryResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectPartsResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectPartSummaryResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectSummaryResponse;
import com.fabbitinc.server.application.project.dto.response.ProjectUserSummaryResponse;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.project.model.Project;
import com.fabbitinc.server.domain.project.model.ProjectMember;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.repository.ProjectMemberRepository;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectQuery {

    private final AuthTokenParser authTokenParser;
    private final ProjectRepository projectRepository;
    private final ProjectPartRepository projectPartRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional(readOnly = true)
    public ProjectListResponse listProjects(
            String authorizationHeader,
            String search,
            int offset,
            int limit
    ) {
        authTokenParser.requireAuth(authorizationHeader);
        String normalizedSearch = normalizeSearch(search);

        List<Project> projects = projectRepository.listProjectsPaginated(normalizedSearch, offset, limit);
        long total = projectRepository.countProjects(normalizedSearch);

        List<ProjectSummaryResponse> items = projects.stream()
                .map(this::toProjectSummary)
                .toList();
        return new ProjectListResponse(total, offset, limit, items);
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectDetail(String authorizationHeader, UUID projectId) {
        authTokenParser.requireAuth(authorizationHeader);
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Project '" + projectId + "'을(를) 찾을 수 없습니다"
                ));
        return toProjectDetail(project);
    }

    @Transactional(readOnly = true)
    public MemberLookupResponse lookupMembers(
            String authorizationHeader,
            UUID projectId,
            String search,
            int limit
    ) {
        authTokenParser.requireAuth(authorizationHeader);

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        if (members.isEmpty()) {
            return new MemberLookupResponse(List.of());
        }

        List<UUID> userIds = members.stream().map(ProjectMember::getUserId).toList();
        List<User> users = userRepository.findAllByIdInOrderByFullName(userIds);

        String normalizedSearch = normalizeSearch(search);
        List<ProjectUserSummaryResponse> items = users.stream()
                .filter(user -> normalizedSearch == null
                        || user.getFullName().toLowerCase().contains(normalizedSearch.toLowerCase()))
                .limit(limit)
                .map(this::toProjectUserSummary)
                .toList();
        return new MemberLookupResponse(items);
    }

    @Transactional(readOnly = true)
    public ProjectMemberListResponse listMembers(String authorizationHeader, UUID projectId) {
        authTokenParser.requireAuth(authorizationHeader);

        if (projectRepository.findByIdAndDeletedFalse(projectId).isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다");
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        if (members.isEmpty()) {
            return new ProjectMemberListResponse(List.of());
        }

        List<UUID> userIds = members.stream().map(ProjectMember::getUserId).toList();
        List<User> users = userRepository.findAllByIdInOrderByFullName(userIds);
        Map<UUID, User> userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));

        List<ProjectMemberSummaryResponse> items = members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    if (user == null) {
                        return new ProjectMemberSummaryResponse(
                                member.getUserId(),
                                "",
                                "",
                                null,
                                null,
                                member.getRole().name()
                        );
                    }
                    return new ProjectMemberSummaryResponse(
                            member.getUserId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey()),
                            member.getRole().name()
                    );
                })
                .toList();
        return new ProjectMemberListResponse(items);
    }

    @Transactional(readOnly = true)
    public PartLookupResponse lookupParts(
            String authorizationHeader,
            UUID projectId,
            String search,
            boolean excludeLinked,
            int limit
    ) {
        authTokenParser.requireAuth(authorizationHeader);

        int fetchSize = Math.max(limit * 5, limit);
        String normalizedSearch = normalizeSearch(search);
        String keyword = normalizedSearch == null ? "" : normalizedSearch;
        List<Part> parts = partRepository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
                keyword,
                keyword,
                PageRequest.of(0, fetchSize)
        );

        Set<UUID> linkedPartIds = excludeLinked
                ? projectPartRepository.findByProjectId(projectId).stream()
                .map(ProjectPart::getPartId)
                .collect(Collectors.toSet())
                : Set.of();

        List<PartLookupItemResponse> items = parts.stream()
                .filter(part -> !excludeLinked || !linkedPartIds.contains(part.getId()))
                .limit(limit)
                .map(part -> new PartLookupItemResponse(part.getId(), part.getPartNumber(), part.getName()))
                .toList();

        return new PartLookupResponse(items);
    }

    @Transactional(readOnly = true)
    public ProjectPartsResponse getProjectParts(
            String authorizationHeader,
            UUID projectId,
            String search,
            int offset,
            int limit
    ) {
        authTokenParser.requireAuth(authorizationHeader);

        projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Project '" + projectId + "'을(를) 찾을 수 없습니다"
                ));

        List<ProjectPart> links = projectPartRepository.findByProjectId(projectId);
        if (links.isEmpty()) {
            return new ProjectPartsResponse(0, List.of());
        }

        List<UUID> partIds = links.stream().map(ProjectPart::getPartId).toList();
        List<Part> parts = partRepository.findByIdInOrderByPartNumberAsc(partIds);
        String normalizedSearch = normalizeSearch(search);
        if (normalizedSearch != null) {
            String lowered = normalizedSearch.toLowerCase();
            parts = parts.stream()
                    .filter(part -> part.getPartNumber().toLowerCase().contains(lowered)
                            || (part.getName() != null && part.getName().toLowerCase().contains(lowered)))
                    .toList();
        }

        long total = parts.size();
        int fromIndex = Math.min(offset, parts.size());
        int toIndex = Math.min(offset + limit, parts.size());
        List<ProjectPartSummaryResponse> items = parts.subList(fromIndex, toIndex).stream()
                .map(part -> new ProjectPartSummaryResponse(part.getId(), part.getPartNumber(), part.getName()))
                .toList();

        return new ProjectPartsResponse(total, items);
    }

    @Transactional(readOnly = true)
    public ActivityListResponse getActivities(
            String authorizationHeader,
            UUID projectId,
            UUID cursor,
            int limit,
            String scope,
            UUID userId
    ) {
        authTokenParser.requireAuth(authorizationHeader);

        List<Activity> filtered = activityRepository.findByTargetTypeAndTargetIdOrderByIdDesc(
                        ActivityTargetType.PROJECT,
                        projectId
                ).stream()
                .filter(activity -> cursor == null || activity.getId().compareTo(cursor) < 0)
                .filter(activity -> scope == null || scope.equals(extractScope(activity.getAction())))
                .filter(activity -> userId == null || userId.equals(activity.getActorId()))
                .limit(limit)
                .toList();

        UUID nextCursor = filtered.size() == limit
                ? filtered.get(filtered.size() - 1).getId()
                : null;

        Set<UUID> actorIds = filtered.stream().map(Activity::getActorId).collect(Collectors.toSet());
        List<User> users = actorIds.isEmpty()
                ? List.of()
                : userRepository.findAllByIdInOrderByFullName(actorIds);
        Map<String, UserSummaryResponse> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(
                    user.getId().toString(),
                    new UserSummaryResponse(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey())
                    )
            );
        }

        List<ActivityResponse> items = filtered.stream()
                .map(activity -> new ActivityResponse(
                        activity.getId(),
                        activity.getAction(),
                        extractScope(activity.getAction()),
                        activity.getActorId(),
                        activity.getDetail(),
                        activity.getCreatedAt()
                ))
                .toList();

        return new ActivityListResponse(items, nextCursor, userMap);
    }

    @Transactional(readOnly = true)
    public PartProjectsResponse getPartProjects(String authorizationHeader, UUID partId) {
        authTokenParser.requireAuth(authorizationHeader);

        partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'을(를) 찾을 수 없습니다"
                ));

        List<ProjectPart> links = projectPartRepository.findByPartId(partId);
        if (links.isEmpty()) {
            return new PartProjectsResponse(0, List.of());
        }

        List<UUID> projectIds = links.stream().map(ProjectPart::getProjectId).toList();
        List<PartProjectSummaryResponse> items = projectRepository.findByIdInAndDeletedFalseOrderByNameAsc(projectIds)
                .stream()
                .map(project -> new PartProjectSummaryResponse(
                        project.getId(),
                        project.getName(),
                        project.getDescription()
                ))
                .toList();
        return new PartProjectsResponse(items.size(), items);
    }

    private ProjectSummaryResponse toProjectSummary(Project project) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                (int) projectPartRepository.countByProjectId(project.getId()),
                project.isArchived()
        );
    }

    private ProjectDetailResponse toProjectDetail(Project project) {
        return new ProjectDetailResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                (int) projectPartRepository.countByProjectId(project.getId()),
                project.isArchived(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private String extractScope(String action) {
        if (action == null) {
            return null;
        }
        int idx = action.indexOf(':');
        if (idx < 0) {
            return action;
        }
        return action.substring(0, idx);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProjectUserSummaryResponse toProjectUserSummary(User user) {
        return new ProjectUserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }
}
