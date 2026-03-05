package com.fabbitinc.server.application.project.query;

import com.fabbitinc.server.application.activity.api.ActivityApi;
import com.fabbitinc.server.application.activity.dto.response.ActivityAction;
import com.fabbitinc.server.application.activity.dto.response.ActivityScope;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.application.project.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectActivitiesCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectDetailCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectListCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectMembersCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectMembersLookupCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectPartsCondition;
import com.fabbitinc.server.application.project.query.condition.ProjectPartsLookupCondition;
import com.fabbitinc.server.application.project.query.result.MemberLookupResult;
import com.fabbitinc.server.application.project.query.result.PartProjectSummaryResult;
import com.fabbitinc.server.application.project.query.result.PartProjectsResult;
import com.fabbitinc.server.application.project.query.result.ProjectActivityListResult;
import com.fabbitinc.server.application.project.query.result.ProjectActivityResult;
import com.fabbitinc.server.application.project.query.result.ProjectActivityUserSummaryResult;
import com.fabbitinc.server.application.project.query.result.ProjectDetailResult;
import com.fabbitinc.server.application.project.query.result.ProjectListResult;
import com.fabbitinc.server.application.project.query.result.ProjectMemberListResult;
import com.fabbitinc.server.application.project.query.result.ProjectMemberSummaryResult;
import com.fabbitinc.server.application.project.query.result.ProjectPartLookupItemResult;
import com.fabbitinc.server.application.project.query.result.ProjectPartLookupResult;
import com.fabbitinc.server.application.project.query.result.ProjectPartSummaryResult;
import com.fabbitinc.server.application.project.query.result.ProjectPartsResult;
import com.fabbitinc.server.application.project.query.result.ProjectSummaryResult;
import com.fabbitinc.server.application.project.query.result.ProjectUserSummaryResult;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.project.model.Project;
import com.fabbitinc.server.domain.project.model.ProjectMember;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.repository.ProjectMemberRepository;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
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
@Transactional(readOnly = true)
public class ProjectQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectRepository projectRepository;
    private final ProjectPartRepository projectPartRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final PartApi partApi;
    private final UserApi userApi;
    private final ActivityApi activityApi;
    private final FileUrlResolver fileUrlResolver;

    public ProjectListResult list(ProjectListCondition condition) {
        currentAuthProvider.getCurrentAuth();
        String normalizedSearch = normalizeSearch(condition.search());

        List<Project> projects = projectRepository.listProjectsPaginated(
                normalizedSearch,
                condition.offset(),
                condition.limit()
        );
        long total = projectRepository.countProjects(normalizedSearch);

        List<ProjectSummaryResult> items = projects.stream()
                .map(this::toProjectSummaryResult)
                .toList();
        return new ProjectListResult(total, condition.offset(), condition.limit(), items);
    }

    public ProjectDetailResult get(ProjectDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();
        Project project = projectRepository.findByIdAndDeletedFalse(condition.projectId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Project '" + condition.projectId() + "'을(를) 찾을 수 없습니다"
                ));
        return toProjectDetailResult(project);
    }

    public MemberLookupResult lookupMembers(ProjectMembersLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        List<ProjectMember> members = projectMemberRepository.findByProjectId(condition.projectId());
        if (members.isEmpty()) {
            return new MemberLookupResult(List.of());
        }

        List<UUID> userIds = members.stream().map(ProjectMember::getUserId).toList();
        List<User> users = userApi.getUsersByIdsOrdered(userIds);

        String normalizedSearch = normalizeSearch(condition.search());
        List<ProjectUserSummaryResult> items = users.stream()
                .filter(user -> normalizedSearch == null
                        || user.getFullName().toLowerCase().contains(normalizedSearch.toLowerCase()))
                .limit(condition.limit())
                .map(this::toProjectUserSummaryResult)
                .toList();
        return new MemberLookupResult(items);
    }

    public ProjectMemberListResult listMembers(ProjectMembersCondition condition) {
        currentAuthProvider.getCurrentAuth();

        if (projectRepository.findByIdAndDeletedFalse(condition.projectId()).isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다");
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectId(condition.projectId());
        if (members.isEmpty()) {
            return new ProjectMemberListResult(List.of());
        }

        List<UUID> userIds = members.stream().map(ProjectMember::getUserId).toList();
        List<User> users = userApi.getUsersByIdsOrdered(userIds);
        Map<UUID, User> userMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));

        List<ProjectMemberSummaryResult> items = members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    if (user == null) {
                        return new ProjectMemberSummaryResult(
                                member.getUserId(),
                                "",
                                "",
                                null,
                                null,
                                member.getRole()
                        );
                    }
                    return new ProjectMemberSummaryResult(
                            member.getUserId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey()),
                            member.getRole()
                    );
                })
                .toList();
        return new ProjectMemberListResult(items);
    }

    public ProjectPartLookupResult lookupParts(ProjectPartsLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        int fetchSize = Math.max(condition.limit() * 5, condition.limit());
        String normalizedSearch = normalizeSearch(condition.search());
        String keyword = normalizedSearch == null ? "" : normalizedSearch;
        List<Part> parts = partApi.searchParts(keyword, fetchSize);

        Set<UUID> linkedPartIds = condition.excludeLinked()
                ? projectPartRepository.findByProjectId(condition.projectId()).stream()
                .map(ProjectPart::getPartId)
                .collect(Collectors.toSet())
                : Set.of();

        List<ProjectPartLookupItemResult> items = parts.stream()
                .filter(part -> !condition.excludeLinked() || !linkedPartIds.contains(part.getId()))
                .limit(condition.limit())
                .map(part -> new ProjectPartLookupItemResult(part.getId(), part.getPartNumber(), part.getName()))
                .toList();

        return new ProjectPartLookupResult(items);
    }

    public ProjectPartsResult listParts(ProjectPartsCondition condition) {
        currentAuthProvider.getCurrentAuth();

        projectRepository.findByIdAndDeletedFalse(condition.projectId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Project '" + condition.projectId() + "'을(를) 찾을 수 없습니다"
                ));

        List<ProjectPart> links = projectPartRepository.findByProjectId(condition.projectId());
        if (links.isEmpty()) {
            return new ProjectPartsResult(0, List.of());
        }

        List<UUID> partIds = links.stream().map(ProjectPart::getPartId).toList();
        List<Part> parts = partApi.getPartsByIdsOrdered(partIds);
        String normalizedSearch = normalizeSearch(condition.search());
        if (normalizedSearch != null) {
            String lowered = normalizedSearch.toLowerCase();
            parts = parts.stream()
                    .filter(part -> part.getPartNumber().toLowerCase().contains(lowered)
                            || (part.getName() != null && part.getName().toLowerCase().contains(lowered)))
                    .toList();
        }

        long total = parts.size();
        int fromIndex = Math.min(condition.offset(), parts.size());
        int toIndex = Math.min(condition.offset() + condition.limit(), parts.size());
        List<ProjectPartSummaryResult> items = parts.subList(fromIndex, toIndex).stream()
                .map(part -> new ProjectPartSummaryResult(part.getId(), part.getPartNumber(), part.getName()))
                .toList();

        return new ProjectPartsResult(total, items);
    }

    public ProjectActivityListResult listActivities(ProjectActivitiesCondition condition) {
        currentAuthProvider.getCurrentAuth();

        List<Activity> filtered = activityApi.listTargetActivities(
                        ActivityTargetType.PROJECT,
                        condition.projectId()
                ).stream()
                .filter(activity -> condition.cursor() == null || activity.getId().compareTo(condition.cursor()) < 0)
                .filter(activity -> condition.scope() == null || condition.scope().equals(extractScope(activity.getAction())))
                .filter(activity -> condition.userId() == null || condition.userId().equals(activity.getActorId()))
                .limit(condition.limit())
                .toList();

        UUID nextCursor = filtered.size() == condition.limit()
                ? filtered.get(filtered.size() - 1).getId()
                : null;

        Set<UUID> actorIds = filtered.stream().map(Activity::getActorId).collect(Collectors.toSet());
        List<User> users = actorIds.isEmpty()
                ? List.of()
                : userApi.getUsersByIdsOrdered(List.copyOf(actorIds));
        Map<String, ProjectActivityUserSummaryResult> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(
                    user.getId().toString(),
                    new ProjectActivityUserSummaryResult(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getPhone(),
                            fileUrlResolver.resolve(user.getProfileImageFileKey())
                    )
            );
        }

        List<ProjectActivityResult> items = filtered.stream()
                .map(activity -> {
                    ActivityAction action = ActivityAction.from(activity.getAction());
                    return new ProjectActivityResult(
                            activity.getId(),
                            action,
                            action.scope(),
                            activity.getActorId(),
                            activity.getDetail(),
                            activity.getCreatedAt()
                    );
                })
                .toList();

        return new ProjectActivityListResult(items, nextCursor, userMap);
    }

    public PartProjectsResult listPartProjects(PartProjectsCondition condition) {
        currentAuthProvider.getCurrentAuth();

        if (!partApi.existsPart(condition.partId())) {
            throw new AppException(ErrorCode.NOT_FOUND, "Part '" + condition.partId() + "'을(를) 찾을 수 없습니다");
        }

        List<ProjectPart> links = projectPartRepository.findByPartId(condition.partId());
        if (links.isEmpty()) {
            return new PartProjectsResult(0, List.of());
        }

        List<UUID> projectIds = links.stream().map(ProjectPart::getProjectId).toList();
        List<PartProjectSummaryResult> items = projectRepository.findByIdInAndDeletedFalseOrderByNameAsc(projectIds)
                .stream()
                .map(project -> new PartProjectSummaryResult(
                        project.getId(),
                        project.getName(),
                        project.getDescription()
                ))
                .toList();
        return new PartProjectsResult(items.size(), items);
    }

    private ProjectSummaryResult toProjectSummaryResult(Project project) {
        return new ProjectSummaryResult(
                project.getId(),
                project.getName(),
                project.getDescription(),
                (int) projectPartRepository.countByProjectId(project.getId()),
                project.isArchived()
        );
    }

    private ProjectDetailResult toProjectDetailResult(Project project) {
        return new ProjectDetailResult(
                project.getId(),
                project.getName(),
                project.getDescription(),
                (int) projectPartRepository.countByProjectId(project.getId()),
                project.isArchived(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ActivityScope extractScope(String action) {
        return ActivityScope.fromAction(action);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProjectUserSummaryResult toProjectUserSummaryResult(User user) {
        return new ProjectUserSummaryResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }
}
