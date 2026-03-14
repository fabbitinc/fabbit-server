package com.fabbitinc.server.application.issue.query;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.part.api.ChangeRequestPartRevisionSnapshot;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import com.fabbitinc.server.application.issue.query.condition.ChangeRequestDetailCondition;
import com.fabbitinc.server.application.issue.query.condition.ChangeRequestListCondition;
import com.fabbitinc.server.application.issue.query.condition.ChangeRequestLookupCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueDetailCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueListCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueLookupCondition;
import com.fabbitinc.server.application.issue.query.condition.IssueTimelineCondition;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestDetailResult;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestListResult;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestLookupResult;
import com.fabbitinc.server.application.issue.query.result.ChangeRequestPartRevisionResult;
import com.fabbitinc.server.application.issue.query.result.IssueDetailResult;
import com.fabbitinc.server.application.issue.query.result.IssueFileItemResult;
import com.fabbitinc.server.application.issue.query.result.IssueListResult;
import com.fabbitinc.server.application.issue.query.result.IssueLookupResult;
import com.fabbitinc.server.application.issue.query.result.IssueTimelineResult;
import com.fabbitinc.server.application.issue.query.result.IssueUserSummaryResult;
import com.fabbitinc.server.application.issue.query.result.LabelBadgeResult;
import com.fabbitinc.server.application.issue.query.result.LinkedChangeRequestBadgeResult;
import com.fabbitinc.server.application.issue.query.result.LinkedIssueBadgeResult;
import com.fabbitinc.server.application.issue.query.result.PartBadgeResult;
import com.fabbitinc.server.application.issue.query.result.ReviewerSummaryResult;
import com.fabbitinc.server.application.issue.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.issue.query.result.TimelineItemTypeResult;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import com.fabbitinc.server.domain.issue.model.ChangeRequestIssue;
import com.fabbitinc.server.domain.issue.model.ChangeRequestReviewer;
import com.fabbitinc.server.domain.issue.model.ChangeRequestTeamReviewer;
import com.fabbitinc.server.domain.issue.model.CrState;
import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.model.IssueAssignee;
import com.fabbitinc.server.domain.issue.model.IssueComment;
import com.fabbitinc.server.domain.issue.model.IssueLabel;
import com.fabbitinc.server.domain.issue.model.IssuePart;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueTeamAssignee;
import com.fabbitinc.server.domain.issue.model.IssueType;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestIssueRepository;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestRepository;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestReviewerRepository;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestTeamReviewerRepository;
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
    private final ChangeRequestRepository changeRequestRepository;
    private final IssueAssigneeRepository issueAssigneeRepository;
    private final IssueTeamAssigneeRepository issueTeamAssigneeRepository;
    private final IssuePartRepository issuePartRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final ChangeRequestReviewerRepository changeRequestReviewerRepository;
    private final ChangeRequestTeamReviewerRepository changeRequestTeamReviewerRepository;
    private final ChangeRequestIssueRepository changeRequestIssueRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LabelRepository labelRepository;
    private final PartApi partApi;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final FileUrlResolver fileUrlResolver;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public IssueLookupResult lookupIssues(IssueLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();
        IssueType requestedType = parseIssueType(condition.type());

        List<Issue> source = issueRepository.findAll(Sort.by(Sort.Direction.DESC, "number"));

        List<IssueLookupResult.Item> items = source.stream()
                .filter(issue -> requestedType == null || issue.getType() == requestedType)
                .filter(issue -> matchesLookupSearch(issue.getNumber(), issue.getTitle(), condition.search()))
                .limit(condition.limit())
                .map(issue -> new IssueLookupResult.Item(
                        issue.getId(),
                        issue.getNumber(),
                        issue.getTitle(),
                        issue.getState(),
                        issue.getType()
                ))
                .toList();

        return new IssueLookupResult(items);
    }

    public IssueListResult listIssues(IssueListCondition condition) {
        currentAuthProvider.getCurrentAuth();

        String normalizedSearch = normalizeSearch(condition.search());
        IssueState requestedState = parseIssueState(condition.state());
        PathBuilder<Issue> issuePath = new PathBuilder<>(Issue.class, "issue");
        BooleanBuilder predicate = buildIssueListPredicate(issuePath, IssueType.ISSUE, requestedState, normalizedSearch);

        Long totalCount = queryFactory()
                .select(issuePath.get("id", UUID.class).count())
                .from(issuePath)
                .where(predicate)
                .fetchOne();
        long total = totalCount == null ? 0L : totalCount;

        List<Issue> paged = queryFactory()
                .selectFrom(issuePath)
                .where(predicate)
                .orderBy(issuePath.getDateTime("createdAt", Instant.class).desc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();
        Enrichment enrichment = enrich(paged);

        List<IssueListResult.Item> items = paged.stream()
                .map(issue -> toIssueSummary(issue, enrichment))
                .toList();

        return new IssueListResult(
                issueRepository.countByTypeAndState(IssueType.ISSUE, IssueState.OPEN),
                issueRepository.countByTypeAndState(IssueType.ISSUE, IssueState.CLOSED),
                total,
                condition.offset(),
                condition.limit(),
                items
        );
    }

    public IssueDetailResult getIssue(IssueDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();

        int issueNumber = condition.issueNumber();
        Issue issue = issueRepository.findByNumberAndType(issueNumber, IssueType.ISSUE)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Issue #" + issueNumber + "을(를) 찾을 수 없습니다"));

        Enrichment enrichment = enrich(List.of(issue));
        return toIssueDetail(issue, enrichment);
    }

    public ChangeRequestLookupResult lookupChangeRequests(ChangeRequestLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        List<ChangeRequestLookupResult.Item> items = changeRequestRepository.findAllByOrderByNumberDesc().stream()
                .filter(changeRequest -> matchesLookupSearch(
                        changeRequest.getNumber(),
                        changeRequest.getTitle(),
                        condition.search()
                ))
                .limit(condition.limit())
                .map(changeRequest -> new ChangeRequestLookupResult.Item(
                        changeRequest.getId(),
                        changeRequest.getNumber(),
                        changeRequest.getTitle(),
                        changeRequest.getState(),
                        changeRequest.getCrState()
                ))
                .toList();

        return new ChangeRequestLookupResult(items);
    }

    public ChangeRequestListResult listChangeRequests(ChangeRequestListCondition condition) {
        currentAuthProvider.getCurrentAuth();

        String normalizedSearch = normalizeSearch(condition.search());
        IssueState requestedState = parseIssueState(condition.state());
        CrState requestedCrState = parseCrState(condition.crState());
        PathBuilder<ChangeRequest> changeRequestPath = new PathBuilder<>(ChangeRequest.class, "changeRequest");
        BooleanBuilder predicate = buildChangeRequestListPredicate(
                changeRequestPath,
                requestedState,
                requestedCrState,
                normalizedSearch
        );

        Long totalCount = queryFactory()
                .select(changeRequestPath.get("id", UUID.class).count())
                .from(changeRequestPath)
                .where(predicate)
                .fetchOne();
        long total = totalCount == null ? 0L : totalCount;

        List<ChangeRequest> paged = queryFactory()
                .selectFrom(changeRequestPath)
                .where(predicate)
                .orderBy(changeRequestPath.getDateTime("createdAt", Instant.class).desc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();
        List<Issue> asIssues = paged.stream().map(item -> (Issue) item).toList();
        Enrichment enrichment = enrich(asIssues);

        List<ChangeRequestListResult.Item> items = paged.stream()
                .map(changeRequest -> toChangeRequestSummary(changeRequest, enrichment))
                .toList();

        return new ChangeRequestListResult(
                issueRepository.countByTypeAndState(IssueType.CHANGE_REQUEST, IssueState.OPEN),
                issueRepository.countByTypeAndState(IssueType.CHANGE_REQUEST, IssueState.CLOSED),
                total,
                condition.offset(),
                condition.limit(),
                items
        );
    }

    public ChangeRequestDetailResult getChangeRequest(ChangeRequestDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();

        int issueNumber = condition.issueNumber();
        ChangeRequest changeRequest = changeRequestRepository.findByNumber(issueNumber)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "ChangeRequest #" + issueNumber + "을(를) 찾을 수 없습니다"
                ));

        Enrichment enrichment = enrich(List.of(changeRequest));
        return toChangeRequestDetail(changeRequest, enrichment);
    }

    public IssueTimelineResult getTimeline(IssueTimelineCondition condition) {
        currentAuthProvider.getCurrentAuth();
        IssueType type = toIssueType(condition.targetType());

        Issue issue = issueRepository.findByNumberAndType(condition.issueNumber(), type)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "이슈를 찾을 수 없습니다"));

        List<IssueComment> comments = issueCommentRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId());
        List<Activity> activities = activityRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ActivityTargetType.ISSUE,
                issue.getId()
        );

        List<IssueTimelineResult.Item> merged = new ArrayList<>();
        for (IssueComment comment : comments) {
            merged.add(new IssueTimelineResult.Item(
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
            merged.add(new IssueTimelineResult.Item(
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
        merged.sort(java.util.Comparator.comparing(IssueTimelineResult.Item::createdAt));

        Set<UUID> userIds = collectTimelineUserIds(comments, activities);
        Map<String, IssueUserSummaryResult> users = toUserSummaryMap(userIds);

        return new IssueTimelineResult(merged, users);
    }

    private Enrichment enrich(List<Issue> issues) {
        if (issues.isEmpty()) {
            return Enrichment.empty();
        }

        List<UUID> issueIds = issues.stream().map(Issue::getId).toList();

        List<IssueLabel> labelLinks = issueLabelRepository.findByIssueIdIn(issueIds);
        Set<UUID> labelIds = labelLinks.stream().map(IssueLabel::getLabelId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, Label> labels = findLabels(labelIds);

        List<IssueAssignee> assigneeLinks = issueAssigneeRepository.findByIssueIdIn(issueIds);
        List<IssueTeamAssignee> teamAssigneeLinks = issueTeamAssigneeRepository.findByIssueIdIn(issueIds);
        List<IssuePart> partLinks = issuePartRepository.findByIssueIdIn(issueIds);
        List<File> files = fileRepository.findByOwnerTypeAndOwnerIdInAndDeletedAtIsNull("issue", issueIds);

        Set<UUID> userIds = new LinkedHashSet<>();
        issues.stream().map(Issue::getCreatedBy).filter(java.util.Objects::nonNull).forEach(userIds::add);
        assigneeLinks.stream().map(IssueAssignee::getUserId).forEach(userIds::add);

        Set<UUID> teamIds = new LinkedHashSet<>();
        teamAssigneeLinks.stream().map(IssueTeamAssignee::getTeamId).forEach(teamIds::add);

        Set<UUID> partIds = partLinks.stream().map(IssuePart::getPartId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, PartSnapshot> partMap = findParts(partIds);

        Set<UUID> changeRequestIds = issues.stream()
                .filter(issue -> issue instanceof ChangeRequest)
                .map(Issue::getId)
                .collect(java.util.stream.Collectors.toSet());

        List<ChangeRequestReviewer> reviewerLinks = changeRequestIds.isEmpty()
                ? List.of()
                : changeRequestReviewerRepository.findByChangeRequestIdIn(changeRequestIds);
        reviewerLinks.stream().map(ChangeRequestReviewer::getUserId).forEach(userIds::add);

        List<ChangeRequestTeamReviewer> teamReviewerLinks = changeRequestIds.isEmpty()
                ? List.of()
                : changeRequestTeamReviewerRepository.findByChangeRequestIdIn(changeRequestIds);
        teamReviewerLinks.stream().map(ChangeRequestTeamReviewer::getTeamId).forEach(teamIds::add);

        Map<UUID, List<ChangeRequestIssue>> linksByCrId = groupByChangeRequestId(
                changeRequestIds.isEmpty() ? List.of() : changeRequestIssueRepository.findByChangeRequestIdIn(changeRequestIds)
        );

        Set<UUID> issueOnlyIds = issues.stream()
                .filter(issue -> issue.getType() == IssueType.ISSUE)
                .map(Issue::getId)
                .collect(java.util.stream.Collectors.toSet());

        Map<UUID, List<ChangeRequestIssue>> linksByIssueId = groupByIssueId(
                issueOnlyIds.isEmpty() ? List.of() : changeRequestIssueRepository.findByIssueIdIn(issueOnlyIds)
        );

        Set<UUID> linkedIssueIds = linksByCrId.values().stream()
                .flatMap(List::stream)
                .map(ChangeRequestIssue::getIssueId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> linkedCrIds = linksByIssueId.values().stream()
                .flatMap(List::stream)
                .map(ChangeRequestIssue::getChangeRequestId)
                .collect(java.util.stream.Collectors.toSet());

        Map<UUID, Issue> linkedIssueMap = new HashMap<>();
        issueRepository.findAllById(linkedIssueIds).forEach(linked -> linkedIssueMap.put(linked.getId(), linked));

        Map<UUID, ChangeRequest> linkedCrMap = new HashMap<>();
        changeRequestRepository.findAllById(linkedCrIds).forEach(linked -> linkedCrMap.put(linked.getId(), linked));

        Map<UUID, Team> teamMap = findTeams(teamIds);
        Map<UUID, User> userMap = findUsers(userIds);
        Map<UUID, Long> commentCounts = countComments(issueIds);

        return new Enrichment(
                labels,
                labelLinks,
                assigneeLinks,
                teamAssigneeLinks,
                partLinks,
                files,
                reviewerLinks,
                teamReviewerLinks,
                linksByCrId,
                linksByIssueId,
                linkedIssueMap,
                linkedCrMap,
                teamMap,
                userMap,
                partMap,
                commentCounts
        );
    }

    private IssueListResult.Item toIssueSummary(Issue issue, Enrichment enrichment) {
        return new IssueListResult.Item(
                issue.getId(),
                issue.getNumber(),
                issue.getType(),
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

    private ChangeRequestListResult.Item toChangeRequestSummary(ChangeRequest changeRequest, Enrichment enrichment) {
        return new ChangeRequestListResult.Item(
                changeRequest.getId(),
                changeRequest.getNumber(),
                changeRequest.getType(),
                changeRequest.getTitle(),
                changeRequest.getState(),
                changeRequest.getClosedAt(),
                changeRequest.getCreatedAt(),
                changeRequest.getUpdatedAt(),
                toUserSummary(enrichment.userMap().get(changeRequest.getCreatedBy())),
                labelsOf(changeRequest.getId(), enrichment),
                assigneesOf(changeRequest.getId(), enrichment),
                assignedTeamsOf(changeRequest.getId(), enrichment),
                reviewersOf(changeRequest.getId(), enrichment),
                reviewerTeamsOf(changeRequest.getId(), enrichment),
                partsOf(changeRequest.getId(), enrichment),
                filesOf(changeRequest.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(changeRequest.getId(), 0L).intValue(),
                changeRequest.getCrState(),
                changeRequest.getMergedAt(),
                changeRequest.getMergedBy()
        );
    }

    private IssueDetailResult toIssueDetail(Issue issue, Enrichment enrichment) {
        return new IssueDetailResult(
                issue.getId(),
                issue.getNumber(),
                issue.getType(),
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
                linkedChangesOf(issue.getId(), enrichment)
        );
    }

    private ChangeRequestDetailResult toChangeRequestDetail(ChangeRequest changeRequest, Enrichment enrichment) {
        return new ChangeRequestDetailResult(
                changeRequest.getId(),
                changeRequest.getNumber(),
                changeRequest.getType(),
                changeRequest.getTitle(),
                parseJson(changeRequest.getBody()),
                changeRequest.getState(),
                changeRequest.getClosedAt(),
                changeRequest.getCreatedAt(),
                changeRequest.getUpdatedAt(),
                isModified(changeRequest.getCreatedAt(), changeRequest.getUpdatedAt()),
                toUserSummary(enrichment.userMap().get(changeRequest.getCreatedBy())),
                labelsOf(changeRequest.getId(), enrichment),
                assigneesOf(changeRequest.getId(), enrichment),
                assignedTeamsOf(changeRequest.getId(), enrichment),
                reviewersOf(changeRequest.getId(), enrichment),
                reviewerTeamsOf(changeRequest.getId(), enrichment),
                partsOf(changeRequest.getId(), enrichment),
                partRevisionsOf(changeRequest.getId()),
                filesOf(changeRequest.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(changeRequest.getId(), 0L).intValue(),
                changeRequest.getCrState(),
                changeRequest.getMergedAt(),
                changeRequest.getMergedBy(),
                linkedIssuesOf(changeRequest.getId(), enrichment)
        );
    }

    private List<LabelBadgeResult> labelsOf(UUID issueId, Enrichment enrichment) {
        List<LabelBadgeResult> result = new ArrayList<>();
        for (IssueLabel link : enrichment.labelLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            Label label = enrichment.labels().get(link.getLabelId());
            if (label == null) {
                continue;
            }
            result.add(new LabelBadgeResult(label.getId(), label.getName(), label.getColor()));
        }
        return result;
    }

    private List<IssueUserSummaryResult> assigneesOf(UUID issueId, Enrichment enrichment) {
        List<IssueUserSummaryResult> result = new ArrayList<>();
        for (IssueAssignee link : enrichment.assigneeLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            result.add(toUserSummary(enrichment.userMap().get(link.getUserId())));
        }
        return result.stream().filter(java.util.Objects::nonNull).toList();
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

    private List<ChangeRequestPartRevisionResult> partRevisionsOf(UUID changeRequestId) {
        return partRevisionWorkflowApi.listChangeRequestPartRevisions(changeRequestId).stream()
                .map(this::toChangeRequestPartRevisionResult)
                .toList();
    }

    private ChangeRequestPartRevisionResult toChangeRequestPartRevisionResult(ChangeRequestPartRevisionSnapshot snapshot) {
        return new ChangeRequestPartRevisionResult(
                snapshot.revisionId(),
                snapshot.partId(),
                snapshot.partNumber(),
                snapshot.baseRevisionCode(),
                snapshot.draftKey(),
                snapshot.name(),
                snapshot.status()
        );
    }

    private List<IssueFileItemResult> filesOf(UUID issueId, Enrichment enrichment) {
        return enrichment.files().stream()
                .filter(file -> issueId.equals(file.getOwnerId()))
                .map(this::toFileItem)
                .toList();
    }

    private List<ReviewerSummaryResult> reviewersOf(UUID changeRequestId, Enrichment enrichment) {
        List<ReviewerSummaryResult> result = new ArrayList<>();
        for (ChangeRequestReviewer reviewer : enrichment.reviewerLinks()) {
            if (!changeRequestId.equals(reviewer.getChangeRequestId())) {
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

    private List<TeamBadgeResult> reviewerTeamsOf(UUID changeRequestId, Enrichment enrichment) {
        List<TeamBadgeResult> result = new ArrayList<>();
        for (ChangeRequestTeamReviewer reviewer : enrichment.teamReviewerLinks()) {
            if (!changeRequestId.equals(reviewer.getChangeRequestId())) {
                continue;
            }
            Team team = enrichment.teamMap().get(reviewer.getTeamId());
            if (team != null) {
                result.add(new TeamBadgeResult(team.getId(), team.getName()));
            }
        }
        return result;
    }

    private List<LinkedIssueBadgeResult> linkedIssuesOf(UUID changeRequestId, Enrichment enrichment) {
        List<LinkedIssueBadgeResult> result = new ArrayList<>();
        for (ChangeRequestIssue link : enrichment.linksByCrId().getOrDefault(changeRequestId, List.of())) {
            Issue issue = enrichment.linkedIssueMap().get(link.getIssueId());
            if (issue == null) {
                continue;
            }
            result.add(new LinkedIssueBadgeResult(issue.getId(), issue.getNumber(), issue.getTitle(), issue.getState()));
        }
        return result;
    }

    private List<LinkedChangeRequestBadgeResult> linkedChangesOf(UUID issueId, Enrichment enrichment) {
        List<LinkedChangeRequestBadgeResult> result = new ArrayList<>();
        for (ChangeRequestIssue link : enrichment.linksByIssueId().getOrDefault(issueId, List.of())) {
            ChangeRequest changeRequest = enrichment.linkedCrMap().get(link.getChangeRequestId());
            if (changeRequest == null) {
                continue;
            }
            result.add(new LinkedChangeRequestBadgeResult(
                    changeRequest.getId(),
                    changeRequest.getNumber(),
                    changeRequest.getTitle(),
                    changeRequest.getState(),
                    changeRequest.getCrState()
            ));
        }
        return result;
    }

    private Set<UUID> collectTimelineUserIds(List<IssueComment> comments, List<Activity> activities) {
        Set<UUID> userIds = new LinkedHashSet<>();

        for (IssueComment comment : comments) {
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

        Map<String, IssueUserSummaryResult> map = new LinkedHashMap<>();
        for (User user : userRepository.findByIdInOrderByFullNameAsc(userIds)) {
            map.put(user.getId().toString(), toUserSummary(user));
        }
        return map;
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

    private Map<UUID, Label> findLabels(Set<UUID> labelIds) {
        if (labelIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Label> map = new HashMap<>();
        labelRepository.findAllById(labelIds).forEach(label -> map.put(label.getId(), label));
        return map;
    }

    private Map<UUID, Team> findTeams(Set<UUID> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Team> map = new HashMap<>();
        teamRepository.findAllById(teamIds).forEach(team -> map.put(team.getId(), team));
        return map;
    }

    private Map<UUID, User> findUsers(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, User> map = new HashMap<>();
        userRepository.findByIdInOrderByFullNameAsc(userIds).forEach(user -> map.put(user.getId(), user));
        return map;
    }

    private Map<UUID, PartSnapshot> findParts(Set<UUID> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        return partApi.getPartSnapshotMap(partIds);
    }

    private Map<UUID, Long> countComments(List<UUID> issueIds) {
        if (issueIds.isEmpty()) {
            return Map.of();
        }

        PathBuilder<IssueComment> comment = new PathBuilder<>(IssueComment.class, "issueComment");
        var issueIdExpr = comment.get("issueId", UUID.class);
        var countExpr = comment.get("id", UUID.class).count();

        Map<UUID, Long> counts = new HashMap<>();
        for (Tuple row : queryFactory()
                .select(issueIdExpr, countExpr)
                .from(comment)
                .where(issueIdExpr.in(issueIds))
                .groupBy(issueIdExpr)
                .fetch()) {
            counts.put(row.get(issueIdExpr), row.get(countExpr) == null ? 0L : row.get(countExpr));
        }
        return counts;
    }

    private Map<UUID, List<ChangeRequestIssue>> groupByChangeRequestId(List<ChangeRequestIssue> links) {
        Map<UUID, List<ChangeRequestIssue>> map = new HashMap<>();
        for (ChangeRequestIssue link : links) {
            map.computeIfAbsent(link.getChangeRequestId(), ignored -> new ArrayList<>()).add(link);
        }
        return map;
    }

    private Map<UUID, List<ChangeRequestIssue>> groupByIssueId(List<ChangeRequestIssue> links) {
        Map<UUID, List<ChangeRequestIssue>> map = new HashMap<>();
        for (ChangeRequestIssue link : links) {
            map.computeIfAbsent(link.getIssueId(), ignored -> new ArrayList<>()).add(link);
        }
        return map;
    }

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    private BooleanBuilder buildIssueListPredicate(
            PathBuilder<Issue> issue,
            IssueType type,
            IssueState state,
            String search
    ) {
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(issue.getEnum("type", IssueType.class).eq(type));
        if (state != null) {
            predicate.and(issue.getEnum("state", IssueState.class).eq(state));
        }
        if (search != null) {
            predicate.and(issue.getString("title").containsIgnoreCase(search));
        }
        return predicate;
    }

    private BooleanBuilder buildChangeRequestListPredicate(
            PathBuilder<ChangeRequest> changeRequest,
            IssueState state,
            CrState crState,
            String search
    ) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (state != null) {
            predicate.and(changeRequest.getEnum("state", IssueState.class).eq(state));
        }
        if (crState != null) {
            predicate.and(changeRequest.getEnum("crState", CrState.class).eq(crState));
        }
        if (search != null) {
            predicate.and(changeRequest.getString("title").containsIgnoreCase(search));
        }
        return predicate;
    }

    private IssueType toIssueType(IssueTargetType targetType) {
        if (targetType == IssueTargetType.CHANGE_REQUEST) {
            return IssueType.CHANGE_REQUEST;
        }
        return IssueType.ISSUE;
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

        String lowered = search.toLowerCase();
        if (title != null && title.toLowerCase().contains(lowered)) {
            return true;
        }
        return String.valueOf(number).contains(search);
    }

    private IssueType parseIssueType(String rawType) {
        return parseEnum(rawType, IssueType.class, "type");
    }

    private IssueState parseIssueState(String rawState) {
        return parseEnum(rawState, IssueState.class, "state");
    }

    private CrState parseCrState(String rawCrState) {
        return parseEnum(rawCrState, CrState.class, "cr_state");
    }

    private <T extends Enum<T>> T parseEnum(String rawValue, Class<T> enumType, String fieldName) {
        String normalized = normalizeSearch(rawValue);
        if (normalized == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, normalized.toUpperCase(java.util.Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    fieldName + " 값이 올바르지 않습니다: " + rawValue
            );
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
            ActivityAction.ISSUE_ASSIGNEE_CHANGED,
            ActivityAction.ISSUE_REVIEWER_CHANGED
    );

    private record Enrichment(
            Map<UUID, Label> labels,
            List<IssueLabel> labelLinks,
            List<IssueAssignee> assigneeLinks,
            List<IssueTeamAssignee> teamAssigneeLinks,
            List<IssuePart> partLinks,
            List<File> files,
            List<ChangeRequestReviewer> reviewerLinks,
            List<ChangeRequestTeamReviewer> teamReviewerLinks,
            Map<UUID, List<ChangeRequestIssue>> linksByCrId,
            Map<UUID, List<ChangeRequestIssue>> linksByIssueId,
            Map<UUID, Issue> linkedIssueMap,
            Map<UUID, ChangeRequest> linkedCrMap,
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
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of()
            );
        }
    }
}
