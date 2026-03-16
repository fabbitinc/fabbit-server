package com.fabbitinc.server.application.issue.query;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.issue.query.condition.EngineeringChangeDetailCondition;
import com.fabbitinc.server.application.issue.query.condition.EngineeringChangeListCondition;
import com.fabbitinc.server.application.issue.query.condition.EngineeringChangeLookupCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueTimelineCondition;
import com.fabbitinc.server.application.issue.query.result.EngineeringChangeDetailResult;
import com.fabbitinc.server.application.issue.query.result.EngineeringChangeListResult;
import com.fabbitinc.server.application.issue.query.result.EngineeringChangeLookupResult;
import com.fabbitinc.server.application.issue.query.result.EngineeringChangePartRevisionResult;
import com.fabbitinc.server.application.issue.query.result.IssueFileItemResult;
import com.fabbitinc.server.application.issue.query.result.IssueTimelineResult;
import com.fabbitinc.server.application.issue.query.result.IssueUserSummaryResult;
import com.fabbitinc.server.application.issue.query.result.LinkedIssueBadgeResult;
import com.fabbitinc.server.application.issue.query.result.ReviewerSummaryResult;
import com.fabbitinc.server.application.issue.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.issue.query.result.TimelineItemTypeResult;
import com.fabbitinc.server.application.part.api.EngineeringChangePartRevisionSnapshot;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.issue.model.AbstractComment;
import com.fabbitinc.server.domain.issue.model.EngineeringChange;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeComment;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeReviewer;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeState;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeTeamReviewer;
import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeReviewerRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeTeamReviewerRepository;
import com.fabbitinc.server.domain.issue.repository.IssueRepository;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EngineeringChangeQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeRepository engineeringChangeRepository;
    private final EngineeringChangeCommentRepository engineeringChangeCommentRepository;
    private final EngineeringChangeReviewerRepository engineeringChangeReviewerRepository;
    private final EngineeringChangeTeamReviewerRepository engineeringChangeTeamReviewerRepository;
    private final EngineeringChangeIssueLinkRepository engineeringChangeIssueLinkRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;
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
                        change.getState(),
                        change.getEngineeringChangeState()
                ))
                .toList();

        return new EngineeringChangeLookupResult(items);
    }

    public EngineeringChangeListResult listEngineeringChanges(EngineeringChangeListCondition condition) {
        currentAuthProvider.getCurrentAuth();

        String normalizedSearch = normalizeSearch(condition.search());
        IssueState requestedState = parseIssueState(condition.state());
        EngineeringChangeState requestedEngineeringChangeState = parseEngineeringChangeState(condition.engineeringChangeState());

        PathBuilder<EngineeringChange> engineeringChange = new PathBuilder<>(EngineeringChange.class, "engineeringChange");
        BooleanBuilder predicate = buildEngineeringChangeListPredicate(
                engineeringChange,
                requestedState,
                requestedEngineeringChangeState,
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
                .filter(item -> item.getState() == IssueState.OPEN)
                .count();
        long closedCount = engineeringChangeRepository.findAll().stream()
                .filter(item -> item.getState() == IssueState.CLOSED)
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

    public EngineeringChangeDetailResult getEngineeringChange(EngineeringChangeDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange = engineeringChangeRepository.findByNumber(condition.issueNumber())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "EngineeringChange #" + condition.issueNumber() + "을(를) 찾을 수 없습니다"
                ));

        Enrichment enrichment = enrichEngineeringChanges(List.of(engineeringChange));
        return toEngineeringChangeDetail(engineeringChange, enrichment);
    }

    public IssueTimelineResult getTimeline(int issueNumber) {
        return getTimeline(new IssueTimelineCondition(issueNumber));
    }

    public IssueTimelineResult getTimeline(IssueTimelineCondition condition) {
        currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange = engineeringChangeRepository.findByNumber(condition.issueNumber())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "변경관리를 찾을 수 없습니다"));

        List<EngineeringChangeComment> comments =
                engineeringChangeCommentRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChange.getId());
        List<Activity> activities = activityRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ActivityTargetType.ENGINEERING_CHANGE,
                engineeringChange.getId()
        );

        List<IssueTimelineResult.Item> items = new ArrayList<>();
        for (EngineeringChangeComment comment : comments) {
            items.add(new IssueTimelineResult.Item(
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
        for (Activity activity : activities) {
            ActivityAction action = ActivityAction.from(activity.getAction());
            items.add(new IssueTimelineResult.Item(
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
        items.sort(java.util.Comparator.comparing(IssueTimelineResult.Item::createdAt));

        Map<String, IssueUserSummaryResult> users = toUserSummaryMap(collectTimelineUserIds(comments, activities));
        return new IssueTimelineResult(items, users);
    }

    private Enrichment enrichEngineeringChanges(List<EngineeringChange> engineeringChanges) {
        if (engineeringChanges.isEmpty()) {
            return Enrichment.empty();
        }

        List<UUID> engineeringChangeIds = engineeringChanges.stream().map(EngineeringChange::getId).toList();
        List<EngineeringChangeReviewer> reviewerLinks =
                engineeringChangeReviewerRepository.findByEngineeringChangeIdIn(new LinkedHashSet<>(engineeringChangeIds));
        List<EngineeringChangeTeamReviewer> teamReviewerLinks =
                engineeringChangeTeamReviewerRepository.findByEngineeringChangeIdIn(new LinkedHashSet<>(engineeringChangeIds));
        List<EngineeringChangeIssueLink> issueLinks =
                engineeringChangeIssueLinkRepository.findByEngineeringChangeIdIn(new LinkedHashSet<>(engineeringChangeIds));
        List<File> files = fileRepository.findByOwnerTypeAndOwnerIdInAndDeletedAtIsNull("engineering_change", engineeringChangeIds);

        Set<UUID> userIds = new LinkedHashSet<>();
        for (EngineeringChange engineeringChange : engineeringChanges) {
            if (engineeringChange.getCreatedBy() != null) {
                userIds.add(engineeringChange.getCreatedBy());
            }
        }
        for (EngineeringChangeReviewer reviewer : reviewerLinks) {
            userIds.add(reviewer.getUserId());
        }

        Set<UUID> teamIds = new LinkedHashSet<>();
        for (EngineeringChangeTeamReviewer reviewer : teamReviewerLinks) {
            teamIds.add(reviewer.getTeamId());
        }

        Set<UUID> issueIds = new LinkedHashSet<>();
        for (EngineeringChangeIssueLink link : issueLinks) {
            issueIds.add(link.getIssueId());
        }

        Map<UUID, Issue> linkedIssues = new HashMap<>();
        issueRepository.findAllById(issueIds).forEach(issue -> linkedIssues.put(issue.getId(), issue));

        return new Enrichment(
                reviewerLinks,
                teamReviewerLinks,
                groupByEngineeringChangeId(issueLinks),
                linkedIssues,
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
                reviewersOf(engineeringChange.getId(), enrichment),
                reviewerTeamsOf(engineeringChange.getId(), enrichment),
                filesOf(engineeringChange.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(engineeringChange.getId(), 0L).intValue(),
                engineeringChange.getEngineeringChangeState(),
                engineeringChange.getMergedAt(),
                engineeringChange.getMergedBy()
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
                reviewersOf(engineeringChange.getId(), enrichment),
                reviewerTeamsOf(engineeringChange.getId(), enrichment),
                partRevisionsOf(engineeringChange.getId()),
                filesOf(engineeringChange.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(engineeringChange.getId(), 0L).intValue(),
                engineeringChange.getEngineeringChangeState(),
                engineeringChange.getMergedAt(),
                engineeringChange.getMergedBy(),
                linkedIssuesOf(engineeringChange.getId(), enrichment)
        );
    }

    private List<ReviewerSummaryResult> reviewersOf(UUID engineeringChangeId, Enrichment enrichment) {
        List<ReviewerSummaryResult> result = new ArrayList<>();
        for (EngineeringChangeReviewer reviewer : enrichment.reviewerLinks()) {
            if (!engineeringChangeId.equals(reviewer.getEngineeringChangeId())) {
                continue;
            }
            User user = enrichment.userMap().get(reviewer.getUserId());
            if (user == null) {
                continue;
            }
            result.add(new ReviewerSummaryResult(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    fileUrlResolver.resolve(user.getProfileImageFileKey()),
                    reviewer.getReviewStatus(),
                    reviewer.getReviewedAt()
            ));
        }
        return result;
    }

    private List<TeamBadgeResult> reviewerTeamsOf(UUID engineeringChangeId, Enrichment enrichment) {
        List<TeamBadgeResult> result = new ArrayList<>();
        for (EngineeringChangeTeamReviewer reviewer : enrichment.teamReviewerLinks()) {
            if (!engineeringChangeId.equals(reviewer.getEngineeringChangeId())) {
                continue;
            }
            Team team = enrichment.teamMap().get(reviewer.getTeamId());
            if (team != null) {
                result.add(new TeamBadgeResult(team.getId(), team.getName()));
            }
        }
        return result;
    }

    private List<EngineeringChangePartRevisionResult> partRevisionsOf(UUID engineeringChangeId) {
        return partRevisionWorkflowApi.listEngineeringChangePartRevisions(engineeringChangeId).stream()
                .map(this::toEngineeringChangePartRevisionResult)
                .toList();
    }

    private EngineeringChangePartRevisionResult toEngineeringChangePartRevisionResult(EngineeringChangePartRevisionSnapshot snapshot) {
        return new EngineeringChangePartRevisionResult(
                snapshot.revisionId(),
                snapshot.partId(),
                snapshot.partNumber(),
                snapshot.baseRevisionCode(),
                snapshot.draftKey(),
                snapshot.name(),
                snapshot.status()
        );
    }

    private List<IssueFileItemResult> filesOf(UUID engineeringChangeId, Enrichment enrichment) {
        return enrichment.files().stream()
                .filter(file -> engineeringChangeId.equals(file.getOwnerId()))
                .map(this::toFileItem)
                .toList();
    }

    private List<LinkedIssueBadgeResult> linkedIssuesOf(UUID engineeringChangeId, Enrichment enrichment) {
        List<LinkedIssueBadgeResult> result = new ArrayList<>();
        for (EngineeringChangeIssueLink link : enrichment.linksByEngineeringChangeId().getOrDefault(engineeringChangeId, List.of())) {
            Issue issue = enrichment.linkedIssues().get(link.getIssueId());
            if (issue == null) {
                continue;
            }
            result.add(new LinkedIssueBadgeResult(issue.getId(), issue.getNumber(), issue.getTitle(), issue.getState()));
        }
        return result;
    }

    private Set<UUID> collectTimelineUserIds(List<? extends AbstractComment> comments, List<Activity> activities) {
        Set<UUID> userIds = new LinkedHashSet<>();

        for (AbstractComment comment : comments) {
            if (comment.getCreatedBy() != null) {
                userIds.add(comment.getCreatedBy());
            }
        }

        for (Activity activity : activities) {
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

    private Map<String, IssueUserSummaryResult> toUserSummaryMap(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<String, IssueUserSummaryResult> result = new LinkedHashMap<>();
        for (User user : userRepository.findByIdInOrderByFullNameAsc(userIds)) {
            result.put(user.getId().toString(), toUserSummary(user));
        }
        return result;
    }

    private IssueUserSummaryResult toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new IssueUserSummaryResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private IssueFileItemResult toFileItem(File file) {
        return new IssueFileItemResult(
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

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    private BooleanBuilder buildEngineeringChangeListPredicate(
            PathBuilder<EngineeringChange> engineeringChange,
            IssueState state,
            EngineeringChangeState engineeringChangeState,
            String search
    ) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (state != null) {
            predicate.and(engineeringChange.getEnum("state", IssueState.class).eq(state));
        }
        if (engineeringChangeState != null) {
            predicate.and(engineeringChange.getEnum("engineeringChangeState", EngineeringChangeState.class).eq(engineeringChangeState));
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

    private IssueState parseIssueState(String rawState) {
        return parseEnum(rawState, IssueState.class, "state");
    }

    private EngineeringChangeState parseEngineeringChangeState(String rawEngineeringChangeState) {
        return parseEnum(rawEngineeringChangeState, EngineeringChangeState.class, "engineering_change_state");
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
            ActivityAction.ISSUE_REVIEWER_CHANGED
    );

    private record Enrichment(
            List<EngineeringChangeReviewer> reviewerLinks,
            List<EngineeringChangeTeamReviewer> teamReviewerLinks,
            Map<UUID, List<EngineeringChangeIssueLink>> linksByEngineeringChangeId,
            Map<UUID, Issue> linkedIssues,
            Map<UUID, Team> teamMap,
            Map<UUID, User> userMap,
            List<File> files,
            Map<UUID, Long> commentCounts
    ) {
        private static Enrichment empty() {
            return new Enrichment(
                    List.of(),
                    List.of(),
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
