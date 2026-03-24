package com.fabbitinc.server.application.engineeringchange.query;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeDetailCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeListCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeLookupCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeTimelineCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.ProjectChangeListCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeDetailResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeListResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeLookupResult;

import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeStepResult;
import com.fabbitinc.server.application.engineeringchange.query.result.LinkedIssueSummaryResult;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.issue.api.IssueSnapshot;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeAffectedItemResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.application.project.api.ProjectApi;
import com.fabbitinc.server.application.workitem.query.TimelineDetailParser;
import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineItemTypeResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeComment;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EngineeringChangeQuery {

    private static final Set<ActivityAction> ACTION_WITH_USER_REFS = Set.of(
            ActivityAction.ISSUE_REVIEWER_CHANGED,
            ActivityAction.ENGINEERING_CHANGE_STEP_CHANGED
    );

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeRepository engineeringChangeRepository;
    private final EngineeringChangeCommentRepository engineeringChangeCommentRepository;
    private final EngineeringChangeStepRepository engineeringChangeStepRepository;
    private final EngineeringChangeIssueLinkRepository engineeringChangeIssueLinkRepository;
    private final IssueApi issueApi;
    private final ProjectApi projectApi;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final EngineeringChangeAffectedItemRepository affectedItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final FileUrlResolver fileUrlResolver;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public EngineeringChangeLookupResult lookupEngineeringChanges(EngineeringChangeLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        List<EngineeringChangeLookupResult.Item> items = engineeringChangeRepository.findAllByOrderByNumberDesc().stream()
                .filter(change -> matchesLookupSearch(change.getNumber(), change.getTitle(), condition.search()))
                .limit(condition.limit())
                .map(change -> new EngineeringChangeLookupResult.Item(
                        change.getId(),
                        change.getNumber(),
                        change.getTitle(),
                        change.getState()
                ))
                .toList();

        return new EngineeringChangeLookupResult(items);
    }

    public EngineeringChangeListResult listEngineeringChanges(EngineeringChangeListCondition condition) {
        currentAuthProvider.getCurrentAuth();

        String normalizedSearch = normalizeSearch(condition.search());
        EngineeringChangeState requestedState = parseEngineeringChangeState(condition.state());

        PathBuilder<EngineeringChange> engineeringChange = new PathBuilder<>(EngineeringChange.class, "engineeringChange");
        BooleanBuilder predicate = buildEngineeringChangeListPredicate(
                engineeringChange,
                requestedState,
                normalizedSearch
        );

        Long totalCount = queryFactory()
                .select(engineeringChange.get("id", UUID.class).count())
                .from(engineeringChange)
                .where(predicate)
                .fetchOne();

        List<EngineeringChange> paged = queryFactory()
                .selectFrom(engineeringChange)
                .where(predicate)
                .orderBy(engineeringChange.getDateTime("createdAt", Instant.class).desc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Enrichment enrichment = enrichEngineeringChanges(paged);
        List<EngineeringChangeListResult.Item> items = paged.stream()
                .map(item -> toEngineeringChangeSummary(item, enrichment))
                .toList();

        long openCount = engineeringChangeRepository.findAll().stream()
                .filter(item -> item.getState() == EngineeringChangeState.DRAFT
                        || item.getState() == EngineeringChangeState.REVIEW_PENDING
                        || item.getState() == EngineeringChangeState.APPROVAL_PENDING
                        || item.getState() == EngineeringChangeState.RELEASE_PENDING)
                .count();
        long closedCount = engineeringChangeRepository.findAll().stream()
                .filter(item -> item.getState() == EngineeringChangeState.RELEASED
                        || item.getState() == EngineeringChangeState.CANCELED)
                .count();

        return new EngineeringChangeListResult(
                openCount,
                closedCount,
                totalCount == null ? 0L : totalCount,
                condition.offset(),
                condition.limit(),
                items
        );
    }

    public EngineeringChangeListResult listProjectChanges(ProjectChangeListCondition condition) {
        currentAuthProvider.getCurrentAuth();
        projectApi.validateProjectId(condition.projectId());

        Set<UUID> projectPartIds = projectApi.getProjectPartIds(condition.projectId());
        if (projectPartIds.isEmpty()) {
            return new EngineeringChangeListResult(0, 0, 0, 0, 0, List.of());
        }

        Set<UUID> issueIds = issueApi.getIssueIdsByPartIds(projectPartIds);
        if (issueIds.isEmpty()) {
            return new EngineeringChangeListResult(0, 0, 0, 0, 0, List.of());
        }

        Set<UUID> engineeringChangeIds = engineeringChangeIssueLinkRepository.findByIssueIdIn(issueIds).stream()
                .map(EngineeringChangeIssueLink::getEngineeringChangeId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (engineeringChangeIds.isEmpty()) {
            return new EngineeringChangeListResult(0, 0, 0, 0, 0, List.of());
        }

        PathBuilder<EngineeringChange> engineeringChange = new PathBuilder<>(EngineeringChange.class, "engineeringChange");
        List<EngineeringChange> engineeringChanges = queryFactory()
                .selectFrom(engineeringChange)
                .where(engineeringChange.get("id", UUID.class).in(engineeringChangeIds))
                .orderBy(engineeringChange.getDateTime("createdAt", Instant.class).desc())
                .fetch();

        Enrichment enrichment = enrichEngineeringChanges(engineeringChanges);
        List<EngineeringChangeListResult.Item> items = engineeringChanges.stream()
                .map(item -> toEngineeringChangeSummary(item, enrichment))
                .toList();

        long openCount = engineeringChanges.stream().filter(item -> isOpenState(item.getState())).count();
        long closedCount = engineeringChanges.stream().filter(item -> isClosedState(item.getState())).count();

        return new EngineeringChangeListResult(
                openCount,
                closedCount,
                items.size(),
                0,
                items.size(),
                items
        );
    }

    public EngineeringChangeDetailResult getEngineeringChange(EngineeringChangeDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange = engineeringChangeRepository.findById(condition.engineeringChangeId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "EngineeringChange '" + condition.engineeringChangeId() + "'을(를) 찾을 수 없습니다"
                ));

        Enrichment enrichment = enrichEngineeringChanges(List.of(engineeringChange));
        return toEngineeringChangeDetail(engineeringChange, enrichment);
    }

    public TimelineResult getTimeline(UUID engineeringChangeId) {
        return getTimeline(new EngineeringChangeTimelineCondition(engineeringChangeId));
    }

    public TimelineResult getTimeline(EngineeringChangeTimelineCondition condition) {
        currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange = engineeringChangeRepository.findById(condition.engineeringChangeId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "변경관리를 찾을 수 없습니다"));

        List<EngineeringChangeComment> comments =
                engineeringChangeCommentRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChange.getId());
        List<Activity> histories = activityRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ActivityTargetType.ENGINEERING_CHANGE,
                engineeringChange.getId()
        );

        Map<String, UserSummaryResult> users = toUserSummaryMap(collectTimelineUserIds(comments, histories));
        List<TimelineResult.Item> items = new ArrayList<>();
        for (EngineeringChangeComment comment : comments) {
            items.add(new TimelineResult.Item(
                    TimelineItemTypeResult.COMMENT,
                    comment.getId(),
                    null,
                    null,
                    null,
                    null,
                    parseJson(comment.getBody()),
                    users.get(comment.getCreatedBy() == null ? null : comment.getCreatedBy().toString()),
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
                    users.get(activity.getActorId() == null ? null : activity.getActorId().toString()),
                    TimelineDetailParser.parse(parseJson(activity.getDetail())),
                    null,
                    null,
                    activity.getCreatedAt(),
                    null,
                    null
            ));
        }
        items.sort(java.util.Comparator.comparing(TimelineResult.Item::createdAt));

        return new TimelineResult(items);
    }

    private Enrichment enrichEngineeringChanges(List<EngineeringChange> engineeringChanges) {
        if (engineeringChanges.isEmpty()) {
            return Enrichment.empty();
        }

        List<UUID> engineeringChangeIds = engineeringChanges.stream().map(EngineeringChange::getId).toList();
        List<EngineeringChangeStep> steps =
                engineeringChangeStepRepository.findByEngineeringChangeIdIn(new LinkedHashSet<>(engineeringChangeIds));
        List<EngineeringChangeIssueLink> issueLinks =
                engineeringChangeIssueLinkRepository.findByEngineeringChangeIdIn(new LinkedHashSet<>(engineeringChangeIds));
        List<File> files = fileRepository.findByOwnerTypeAndOwnerIdInAndDeletedAtIsNull("engineering_change", engineeringChangeIds);

        Set<UUID> userIds = new LinkedHashSet<>();
        Set<UUID> teamIds = new LinkedHashSet<>();
        for (EngineeringChange engineeringChange : engineeringChanges) {
            if (engineeringChange.getCreatedBy() != null) {
                userIds.add(engineeringChange.getCreatedBy());
            }
            if (engineeringChange.getReleasedBy() != null) {
                userIds.add(engineeringChange.getReleasedBy());
            }
        }
        for (EngineeringChangeStep step : steps) {
            if (step.getAssigneeType() == EngineeringChangeStepAssigneeType.USER) {
                userIds.add(step.getAssigneeId());
            }
            if (step.getAssigneeType() == EngineeringChangeStepAssigneeType.TEAM) {
                teamIds.add(step.getAssigneeId());
            }
            if (step.getActedBy() != null) {
                userIds.add(step.getActedBy());
            }
        }

        Set<UUID> issueIds = new LinkedHashSet<>();
        for (EngineeringChangeIssueLink link : issueLinks) {
            issueIds.add(link.getIssueId());
        }

        return new Enrichment(
                groupStepsByEngineeringChangeId(steps),
                groupByEngineeringChangeId(issueLinks),
                new HashMap<>(issueApi.getIssueSnapshotMap(issueIds)),
                findTeams(teamIds),
                findUsers(userIds),
                files,
                countEngineeringChangeComments(engineeringChangeIds)
        );
    }

    private EngineeringChangeListResult.Item toEngineeringChangeSummary(EngineeringChange engineeringChange, Enrichment enrichment) {
        return new EngineeringChangeListResult.Item(
                engineeringChange.getId(),
                engineeringChange.getNumber(),
                engineeringChange.getTitle(),
                engineeringChange.getState(),
                engineeringChange.getClosedAt(),
                engineeringChange.getCreatedAt(),
                engineeringChange.getUpdatedAt(),
                toUserSummary(enrichment.userMap().get(engineeringChange.getCreatedBy())),
                stepsOf(engineeringChange.getId(), enrichment),
                filesOf(engineeringChange.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(engineeringChange.getId(), 0L).intValue(),
                engineeringChange.getReleasedAt(),
                toUserSummary(enrichment.userMap().get(engineeringChange.getReleasedBy()))
        );
    }

    private EngineeringChangeDetailResult toEngineeringChangeDetail(EngineeringChange engineeringChange, Enrichment enrichment) {
        return new EngineeringChangeDetailResult(
                engineeringChange.getId(),
                engineeringChange.getNumber(),
                engineeringChange.getTitle(),
                parseJson(engineeringChange.getBody()),
                engineeringChange.getState(),
                engineeringChange.getClosedAt(),
                engineeringChange.getCreatedAt(),
                engineeringChange.getUpdatedAt(),
                isModified(engineeringChange.getCreatedAt(), engineeringChange.getUpdatedAt()),
                toUserSummary(enrichment.userMap().get(engineeringChange.getCreatedBy())),
                toLinkedIssueSummary(enrichment.linkedIssues().get(engineeringChange.getSourceIssueId())),
                stepsOf(engineeringChange.getId(), enrichment),
                affectedItemsOf(engineeringChange.getId()),
                filesOf(engineeringChange.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(engineeringChange.getId(), 0L).intValue(),
                engineeringChange.getReleasedAt(),
                toUserSummary(enrichment.userMap().get(engineeringChange.getReleasedBy())),
                linkedIssuesOf(engineeringChange.getId(), enrichment)
        );
    }

    private List<EngineeringChangeStepResult> stepsOf(UUID engineeringChangeId, Enrichment enrichment) {
        return enrichment.stepsByEngineeringChangeId()
                .getOrDefault(engineeringChangeId, List.of())
                .stream()
                .sorted(Comparator
                        .comparing(EngineeringChangeStep::getStepType)
                        .thenComparingInt(EngineeringChangeStep::getSequence)
                        .thenComparing(EngineeringChangeStep::getCreatedAt))
                .map(step -> new EngineeringChangeStepResult(
                        step.getId(),
                        step.getStepType(),
                        step.getAssigneeType(),
                        step.getSequence(),
                        step.getStatus(),
                        step.getAssigneeType() == EngineeringChangeStepAssigneeType.USER
                                ? toUserSummary(enrichment.userMap().get(step.getAssigneeId()))
                                : null,
                        step.getAssigneeType() == EngineeringChangeStepAssigneeType.TEAM
                                ? toTeamBadge(enrichment.teamMap().get(step.getAssigneeId()))
                                : null,
                        toUserSummary(enrichment.userMap().get(step.getActedBy())),
                        step.getActedAt()
                ))
                .toList();
    }

    private List<EngineeringChangeAffectedItemResult> affectedItemsOf(UUID engineeringChangeId) {
        List<EngineeringChangeAffectedItem> items = affectedItemRepository
                .findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId);
        return items.stream().map(this::toAffectedItemResult).toList();
    }

    private EngineeringChangeAffectedItemResult toAffectedItemResult(EngineeringChangeAffectedItem item) {
        String partNumber = null;
        String revisionCode = null;
        String name = null;
        String status = null;
        UUID partId = null;
        if (item.getItemType() == EngineeringChangeAffectedItemType.REVISION_RELEASE) {
            PartRevision revision = partRevisionRepository.findById(item.getTargetId()).orElse(null);
            if (revision != null) {
                partNumber = revision.getPartNumber();
                revisionCode = revision.getRevisionCode();
                name = revision.getName();
                status = revision.getStatus().name();
                partId = revision.getPartId();
            }
        }
        return new EngineeringChangeAffectedItemResult(
                item.getId(),
                item.getItemType(),
                item.getTargetId(),
                item.getActionDetail(),
                partId,
                partNumber,
                revisionCode,
                name,
                status
        );
    }

    private List<FileItemResult> filesOf(UUID engineeringChangeId, Enrichment enrichment) {
        return enrichment.files().stream()
                .filter(file -> engineeringChangeId.equals(file.getOwnerId()))
                .map(this::toFileItem)
                .toList();
    }

    private List<LinkedIssueSummaryResult> linkedIssuesOf(UUID engineeringChangeId, Enrichment enrichment) {
        List<LinkedIssueSummaryResult> result = new ArrayList<>();
        for (EngineeringChangeIssueLink link : enrichment.linksByEngineeringChangeId().getOrDefault(engineeringChangeId, List.of())) {
            LinkedIssueSummaryResult summary = toLinkedIssueSummary(enrichment.linkedIssues().get(link.getIssueId()));
            if (summary != null) {
                result.add(summary);
            }
        }
        return result;
    }

    private LinkedIssueSummaryResult toLinkedIssueSummary(IssueSnapshot issue) {
        if (issue == null) {
            return null;
        }
        return new LinkedIssueSummaryResult(issue.id(), issue.number(), issue.title(), issue.state());
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

    private TeamBadgeResult toTeamBadge(Team team) {
        if (team == null) {
            return null;
        }
        return new TeamBadgeResult(team.getId(), team.getName());
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

    private Map<UUID, Long> countEngineeringChangeComments(List<UUID> engineeringChangeIds) {
        if (engineeringChangeIds.isEmpty()) {
            return Map.of();
        }

        PathBuilder<EngineeringChangeComment> comment =
                new PathBuilder<>(EngineeringChangeComment.class, "engineeringChangeComment");
        var engineeringChangeId = comment.get("engineeringChangeId", UUID.class);
        var count = comment.get("id", UUID.class).count();

        Map<UUID, Long> result = new HashMap<>();
        for (Tuple row : queryFactory()
                .select(engineeringChangeId, count)
                .from(comment)
                .where(engineeringChangeId.in(engineeringChangeIds))
                .groupBy(engineeringChangeId)
                .fetch()) {
            result.put(row.get(engineeringChangeId), row.get(count) == null ? 0L : row.get(count));
        }
        return result;
    }

    private Map<UUID, List<EngineeringChangeIssueLink>> groupByEngineeringChangeId(List<EngineeringChangeIssueLink> links) {
        Map<UUID, List<EngineeringChangeIssueLink>> result = new HashMap<>();
        for (EngineeringChangeIssueLink link : links) {
            result.computeIfAbsent(link.getEngineeringChangeId(), ignored -> new ArrayList<>()).add(link);
        }
        return result;
    }

    private Map<UUID, List<EngineeringChangeStep>> groupStepsByEngineeringChangeId(List<EngineeringChangeStep> steps) {
        Map<UUID, List<EngineeringChangeStep>> result = new HashMap<>();
        for (EngineeringChangeStep step : steps) {
            result.computeIfAbsent(step.getEngineeringChangeId(), ignored -> new ArrayList<>()).add(step);
        }
        return result;
    }

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    private BooleanBuilder buildEngineeringChangeListPredicate(
            PathBuilder<EngineeringChange> engineeringChange,
            EngineeringChangeState state,
            String search
    ) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (state != null) {
            predicate.and(engineeringChange.getEnum("state", EngineeringChangeState.class).eq(state));
        }
        if (search != null) {
            predicate.and(engineeringChange.getString("title").containsIgnoreCase(search));
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

    private EngineeringChangeState parseEngineeringChangeState(String rawEngineeringChangeState) {
        return parseEnum(rawEngineeringChangeState, EngineeringChangeState.class, "state");
    }

    private boolean isOpenState(EngineeringChangeState state) {
        return state == EngineeringChangeState.DRAFT
                || state == EngineeringChangeState.REVIEW_PENDING
                || state == EngineeringChangeState.APPROVAL_PENDING
                || state == EngineeringChangeState.RELEASE_PENDING;
    }

    private boolean isClosedState(EngineeringChangeState state) {
        return state == EngineeringChangeState.RELEASED
                || state == EngineeringChangeState.CANCELED;
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

    private record Enrichment(
            Map<UUID, List<EngineeringChangeStep>> stepsByEngineeringChangeId,
            Map<UUID, List<EngineeringChangeIssueLink>> linksByEngineeringChangeId,
            Map<UUID, IssueSnapshot> linkedIssues,
            Map<UUID, Team> teamMap,
            Map<UUID, User> userMap,
            List<File> files,
            Map<UUID, Long> commentCounts
    ) {
        private static Enrichment empty() {
            return new Enrichment(
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    List.of(),
                    Map.of()
            );
        }
    }
}
