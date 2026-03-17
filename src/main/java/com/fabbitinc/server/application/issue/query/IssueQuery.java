package com.fabbitinc.server.application.issue.query;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeApi;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeSnapshot;
import com.fabbitinc.server.application.issue.query.condition.IssueDetailCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueListCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueLookupCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueTimelineCondition;
import com.fabbitinc.server.application.issue.query.result.IssueDetailResult;
import com.fabbitinc.server.application.issue.query.result.LinkedEngineeringChangeSummaryResult;
import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.issue.query.result.IssueListResult;
import com.fabbitinc.server.application.issue.query.result.IssueLookupResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.application.issue.query.result.LabelBadgeResult;
import com.fabbitinc.server.application.issue.query.result.PartBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineItemTypeResult;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.model.IssueAssignee;
import com.fabbitinc.server.domain.issue.model.IssueComment;
import com.fabbitinc.server.domain.issue.model.IssueLabel;
import com.fabbitinc.server.domain.issue.model.IssuePart;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueTeamAssignee;
import com.fabbitinc.server.domain.issue.repository.IssueAssigneeRepository;
import com.fabbitinc.server.domain.issue.repository.IssueCommentRepository;
import com.fabbitinc.server.domain.issue.repository.IssueLabelRepository;
import com.fabbitinc.server.domain.issue.repository.IssuePartRepository;
import com.fabbitinc.server.domain.issue.repository.IssueRepository;
import com.fabbitinc.server.domain.issue.repository.IssueTeamAssigneeRepository;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueRepository issueRepository;
    private final EngineeringChangeApi engineeringChangeApi;
    private final IssueAssigneeRepository issueAssigneeRepository;
    private final IssueTeamAssigneeRepository issueTeamAssigneeRepository;
    private final IssuePartRepository issuePartRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LabelRepository labelRepository;
    private final PartApi partApi;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final FileUrlResolver fileUrlResolver;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public IssueLookupResult lookupIssues(IssueLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        List<IssueLookupResult.Item> items = issueRepository.findAll(Sort.by(Sort.Direction.DESC, "number")).stream()
                .filter(issue -> matchesLookupSearch(issue.getNumber(), issue.getTitle(), condition.search()))
                .limit(condition.limit())
                .map(issue -> new IssueLookupResult.Item(
                        issue.getId(),
                        issue.getNumber(),
                        issue.getTitle(),
                        issue.getState()
                ))
                .toList();

        return new IssueLookupResult(items);
    }

    public IssueListResult listIssues(IssueListCondition condition) {
        currentAuthProvider.getCurrentAuth();

        String normalizedSearch = normalizeSearch(condition.search());
        IssueState requestedState = parseIssueState(condition.state());
        PathBuilder<Issue> issue = new PathBuilder<>(Issue.class, "issue");
        BooleanBuilder predicate = buildIssueListPredicate(issue, requestedState, normalizedSearch);

        Long totalCount = queryFactory()
                .select(issue.get("id", UUID.class).count())
                .from(issue)
                .where(predicate)
                .fetchOne();

        List<Issue> paged = queryFactory()
                .selectFrom(issue)
                .where(predicate)
                .orderBy(issue.getDateTime("createdAt", Instant.class).desc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Enrichment enrichment = enrichIssues(paged);
        List<IssueListResult.Item> items = paged.stream()
                .map(item -> toIssueSummary(item, enrichment))
                .toList();

        return new IssueListResult(
                issueRepository.countByState(IssueState.OPEN),
                issueRepository.countByState(IssueState.CLOSED),
                totalCount == null ? 0L : totalCount,
                condition.offset(),
                condition.limit(),
                items
        );
    }

    public IssueDetailResult getIssue(IssueDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();

        Issue issue = issueRepository.findById(condition.issueId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Issue '" + condition.issueId() + "'을(를) 찾을 수 없습니다"
                ));

        Enrichment enrichment = enrichIssues(List.of(issue));
        return toIssueDetail(issue, enrichment);
    }

    public TimelineResult getTimeline(IssueTimelineCondition condition) {
        currentAuthProvider.getCurrentAuth();

        Issue issue = issueRepository.findById(condition.issueId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "이슈를 찾을 수 없습니다"));

        List<IssueComment> comments = issueCommentRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId());
        List<Activity> histories = activityRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ActivityTargetType.ISSUE,
                issue.getId()
        );

        List<TimelineResult.Item> items = new ArrayList<>();
        for (IssueComment comment : comments) {
            items.add(new TimelineResult.Item(
                    TimelineItemTypeResult.COMMENT,
                    comment.getId(),
                    null,
                    null,
                    null,
                    null,
                    parseJson(comment.getBody()),
                    comment.getCreatedBy(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt(),
                    isModified(comment.getCreatedAt(), comment.getUpdatedAt())
            ));
        }
        for (Activity activity : histories) {
            ActivityAction action = ActivityAction.from(activity.getAction());
            items.add(new TimelineResult.Item(
                    TimelineItemTypeResult.ACTIVITY,
                    activity.getId(),
                    action,
                    action.scope(),
                    activity.getActorId(),
                    parseJson(activity.getDetail()),
                    null,
                    null,
                    activity.getCreatedAt(),
                    null,
                    null
            ));
        }
        items.sort(java.util.Comparator.comparing(TimelineResult.Item::createdAt));

        Map<String, UserSummaryResult> users = toUserSummaryMap(collectTimelineUserIds(comments, histories));
        return new TimelineResult(items, users);
    }

    private Enrichment enrichIssues(List<Issue> issues) {
        if (issues.isEmpty()) {
            return Enrichment.empty();
        }

        List<UUID> issueIds = issues.stream().map(Issue::getId).toList();
        List<IssueLabel> labelLinks = issueLabelRepository.findByIssueIdIn(issueIds);
        List<IssueAssignee> assigneeLinks = issueAssigneeRepository.findByIssueIdIn(issueIds);
        List<IssueTeamAssignee> teamAssigneeLinks = issueTeamAssigneeRepository.findByIssueIdIn(issueIds);
        List<IssuePart> partLinks = issuePartRepository.findByIssueIdIn(issueIds);
        List<File> files = fileRepository.findByOwnerTypeAndOwnerIdInAndDeletedAtIsNull("issue", issueIds);
        Set<UUID> labelIds = new LinkedHashSet<>();
        for (IssueLabel link : labelLinks) {
            labelIds.add(link.getLabelId());
        }

        Set<UUID> userIds = new LinkedHashSet<>();
        for (Issue issue : issues) {
            if (issue.getCreatedBy() != null) {
                userIds.add(issue.getCreatedBy());
            }
        }
        for (IssueAssignee assignee : assigneeLinks) {
            userIds.add(assignee.getUserId());
        }

        Set<UUID> teamIds = new LinkedHashSet<>();
        for (IssueTeamAssignee teamAssignee : teamAssigneeLinks) {
            teamIds.add(teamAssignee.getTeamId());
        }

        Set<UUID> partIds = new LinkedHashSet<>();
        for (IssuePart partLink : partLinks) {
            partIds.add(partLink.getPartId());
        }

        return new Enrichment(
                findLabels(labelIds),
                labelLinks,
                assigneeLinks,
                teamAssigneeLinks,
                partLinks,
                files,
                engineeringChangeApi.getLinkedEngineeringChangeSnapshotMap(Set.copyOf(issueIds)),
                findTeams(teamIds),
                findUsers(userIds),
                findParts(partIds),
                countIssueComments(issueIds)
        );
    }

    private IssueListResult.Item toIssueSummary(Issue issue, Enrichment enrichment) {
        return new IssueListResult.Item(
                issue.getId(),
                issue.getNumber(),
                issue.getTitle(),
                issue.getState(),
                issue.getClosedAt(),
                issue.getCreatedAt(),
                issue.getUpdatedAt(),
                toUserSummary(enrichment.userMap().get(issue.getCreatedBy())),
                labelsOf(issue.getId(), enrichment),
                assigneesOf(issue.getId(), enrichment),
                assignedTeamsOf(issue.getId(), enrichment),
                partsOf(issue.getId(), enrichment),
                filesOf(issue.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(issue.getId(), 0L).intValue()
        );
    }

    private IssueDetailResult toIssueDetail(Issue issue, Enrichment enrichment) {
        return new IssueDetailResult(
                issue.getId(),
                issue.getNumber(),
                issue.getTitle(),
                parseJson(issue.getBody()),
                issue.getState(),
                issue.getClosedAt(),
                issue.getCreatedAt(),
                issue.getUpdatedAt(),
                isModified(issue.getCreatedAt(), issue.getUpdatedAt()),
                toUserSummary(enrichment.userMap().get(issue.getCreatedBy())),
                labelsOf(issue.getId(), enrichment),
                assigneesOf(issue.getId(), enrichment),
                assignedTeamsOf(issue.getId(), enrichment),
                partsOf(issue.getId(), enrichment),
                filesOf(issue.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(issue.getId(), 0L).intValue(),
                linkedEngineeringChangesOf(issue.getId(), enrichment)
        );
    }

    private List<LabelBadgeResult> labelsOf(UUID issueId, Enrichment enrichment) {
        List<LabelBadgeResult> result = new ArrayList<>();
        for (IssueLabel link : enrichment.labelLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            Label label = enrichment.labels().get(link.getLabelId());
            if (label != null) {
                result.add(new LabelBadgeResult(label.getId(), label.getName(), label.getColor()));
            }
        }
        return result;
    }

    private List<UserSummaryResult> assigneesOf(UUID issueId, Enrichment enrichment) {
        List<UserSummaryResult> result = new ArrayList<>();
        for (IssueAssignee link : enrichment.assigneeLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            UserSummaryResult user = toUserSummary(enrichment.userMap().get(link.getUserId()));
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    private List<TeamBadgeResult> assignedTeamsOf(UUID issueId, Enrichment enrichment) {
        List<TeamBadgeResult> result = new ArrayList<>();
        for (IssueTeamAssignee link : enrichment.teamAssigneeLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            Team team = enrichment.teamMap().get(link.getTeamId());
            if (team != null) {
                result.add(new TeamBadgeResult(team.getId(), team.getName()));
            }
        }
        return result;
    }

    private List<PartBadgeResult> partsOf(UUID issueId, Enrichment enrichment) {
        List<PartBadgeResult> result = new ArrayList<>();
        for (IssuePart link : enrichment.partLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            PartSnapshot part = enrichment.partMap().get(link.getPartId());
            if (part != null) {
                result.add(new PartBadgeResult(part.id(), part.partNumber(), part.name()));
            }
        }
        return result;
    }

    private List<FileItemResult> filesOf(UUID issueId, Enrichment enrichment) {
        return enrichment.files().stream()
                .filter(file -> issueId.equals(file.getOwnerId()))
                .map(this::toFileItem)
                .toList();
    }

    private List<LinkedEngineeringChangeSummaryResult> linkedEngineeringChangesOf(UUID issueId, Enrichment enrichment) {
        return enrichment.linkedEngineeringChangesByIssueId().getOrDefault(issueId, List.of()).stream()
                .map(engineeringChange -> new LinkedEngineeringChangeSummaryResult(
                        engineeringChange.id(),
                        engineeringChange.number(),
                        engineeringChange.title(),
                        engineeringChange.state()
                ))
                .toList();
    }

    private Set<UUID> collectTimelineUserIds(List<? extends AbstractComment> comments, List<Activity> histories) {
        Set<UUID> userIds = new LinkedHashSet<>();

        for (AbstractComment comment : comments) {
            if (comment.getCreatedBy() != null) {
                userIds.add(comment.getCreatedBy());
            }
        }

        for (Activity activity : histories) {
            if (activity.getActorId() != null) {
                userIds.add(activity.getActorId());
            }

            ActivityAction action = ActivityAction.from(activity.getAction());
            if (!ACTION_WITH_USER_REFS.contains(action)) {
                continue;
            }

            JsonNode detail = parseJson(activity.getDetail());
            if (detail == null) {
                continue;
            }

            collectUserRefIds(detail.path("added"), userIds);
            collectUserRefIds(detail.path("removed"), userIds);
        }

        return userIds;
    }

    private void collectUserRefIds(JsonNode refs, Set<UUID> userIds) {
        if (!refs.isArray()) {
            return;
        }
        for (JsonNode ref : refs) {
            if (!"user".equals(ref.path("type").asText(null))) {
                continue;
            }
            String rawId = ref.path("id").asText(null);
            if (rawId == null) {
                continue;
            }
            try {
                userIds.add(UUID.fromString(rawId));
            } catch (Exception ignored) {
                // 무시한다.
            }
        }
    }

    private Map<String, UserSummaryResult> toUserSummaryMap(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<String, UserSummaryResult> result = new LinkedHashMap<>();
        for (User user : userRepository.findByIdInOrderByFullNameAsc(userIds)) {
            result.put(user.getId().toString(), toUserSummary(user));
        }
        return result;
    }

    private UserSummaryResult toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private FileItemResult toFileItem(File file) {
        return new FileItemResult(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getFileSize(),
                fileUrlResolver.resolve(file.getFileKey()),
                file.getCreatedAt()
        );
    }

    private Map<UUID, Label> findLabels(Set<UUID> labelIds) {
        if (labelIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Label> labels = new HashMap<>();
        labelRepository.findAllById(labelIds).forEach(label -> labels.put(label.getId(), label));
        return labels;
    }

    private Map<UUID, Team> findTeams(Set<UUID> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Team> teams = new HashMap<>();
        teamRepository.findAllById(teamIds).forEach(team -> teams.put(team.getId(), team));
        return teams;
    }

    private Map<UUID, User> findUsers(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, User> users = new HashMap<>();
        userRepository.findByIdInOrderByFullNameAsc(userIds).forEach(user -> users.put(user.getId(), user));
        return users;
    }

    private Map<UUID, PartSnapshot> findParts(Set<UUID> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        return partApi.getPartSnapshotMap(partIds);
    }

    private Map<UUID, Long> countIssueComments(List<UUID> issueIds) {
        if (issueIds.isEmpty()) {
            return Map.of();
        }

        PathBuilder<IssueComment> comment = new PathBuilder<>(IssueComment.class, "issueComment");
        var issueId = comment.get("issueId", UUID.class);
        var count = comment.get("id", UUID.class).count();

        Map<UUID, Long> result = new HashMap<>();
        for (Tuple row : queryFactory()
                .select(issueId, count)
                .from(comment)
                .where(issueId.in(issueIds))
                .groupBy(issueId)
                .fetch()) {
            result.put(row.get(issueId), row.get(count) == null ? 0L : row.get(count));
        }
        return result;
    }

    private Map<UUID, List<EngineeringChangeIssueLink>> groupByIssueId(List<EngineeringChangeIssueLink> links) {
        Map<UUID, List<EngineeringChangeIssueLink>> result = new HashMap<>();
        for (EngineeringChangeIssueLink link : links) {
            result.computeIfAbsent(link.getIssueId(), ignored -> new ArrayList<>()).add(link);
        }
        return result;
    }

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    private BooleanBuilder buildIssueListPredicate(PathBuilder<Issue> issue, IssueState state, String search) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (state != null) {
            predicate.and(issue.getEnum("state", IssueState.class).eq(state));
        }
        if (search != null) {
            predicate.and(issue.getString("title").containsIgnoreCase(search));
        }
        return predicate;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean matchesLookupSearch(int number, String title, String rawSearch) {
        String search = normalizeSearch(rawSearch);
        if (search == null) {
            return true;
        }

        String lowered = search.toLowerCase(Locale.ROOT);
        if (title != null && title.toLowerCase(Locale.ROOT).contains(lowered)) {
            return true;
        }
        return String.valueOf(number).contains(search);
    }

    private IssueState parseIssueState(String rawState) {
        return parseEnum(rawState, IssueState.class, "state");
    }

    private <T extends Enum<T>> T parseEnum(String rawValue, Class<T> enumType, String fieldName) {
        String normalized = normalizeSearch(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, normalized.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, fieldName + " 값이 올바르지 않습니다: " + rawValue);
        }
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JacksonException ex) {
            return null;
        }
    }

    private boolean isModified(Instant createdAt, Instant updatedAt) {
        if (createdAt == null || updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(createdAt);
    }

    private static final Set<ActivityAction> ACTION_WITH_USER_REFS = Set.of(
            ActivityAction.ISSUE_ASSIGNEE_CHANGED
    );

    private record Enrichment(
            Map<UUID, Label> labels,
            List<IssueLabel> labelLinks,
            List<IssueAssignee> assigneeLinks,
            List<IssueTeamAssignee> teamAssigneeLinks,
            List<IssuePart> partLinks,
            List<File> files,
            Map<UUID, List<EngineeringChangeSnapshot>> linkedEngineeringChangesByIssueId,
            Map<UUID, Team> teamMap,
            Map<UUID, User> userMap,
            Map<UUID, PartSnapshot> partMap,
            Map<UUID, Long> commentCounts
    ) {
        private static Enrichment empty() {
            return new Enrichment(
                    Map.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
        }
    }
}
