package com.fabbitinc.server.application.issue.query;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.activity.dto.response.ActivityAction;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestListResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestLookupItemResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestLookupResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestResponse;
import com.fabbitinc.server.application.issue.dto.response.ChangeRequestSummaryResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueListResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueLookupItemResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueLookupResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueSummaryResponse;
import com.fabbitinc.server.application.issue.dto.response.IssueUserSummaryResponse;
import com.fabbitinc.server.application.issue.dto.response.LabelBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.LinkedChangeRequestBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.LinkedIssueBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.PartBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.ReviewerSummaryResponse;
import com.fabbitinc.server.application.issue.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.application.issue.dto.response.TimelineItemResponse;
import com.fabbitinc.server.application.issue.dto.response.TimelineItemType;
import com.fabbitinc.server.application.issue.dto.response.TimelineResponse;
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
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
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
    private final PartRepository partRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final FileUrlResolver fileUrlResolver;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public IssueLookupResponse lookupIssues(String search,
            IssueType type,
            int limit
    ) {
        currentAuthProvider.getCurrentAuth();

        List<Issue> source = issueRepository.findAll(Sort.by(Sort.Direction.DESC, "number"));

        List<IssueLookupItemResponse> items = source.stream()
                .filter(issue -> type == null || issue.getType() == type)
                .filter(issue -> matchesLookupSearch(issue.getNumber(), issue.getTitle(), search))
                .limit(limit)
                .map(issue -> new IssueLookupItemResponse(
                        issue.getId(),
                        issue.getNumber(),
                        issue.getTitle(),
                        issue.getState(),
                        issue.getType()
                ))
                .toList();

        return new IssueLookupResponse(items);
    }

    @Transactional(readOnly = true)
    public IssueListResponse listIssues(String search,
            IssueState state,
            int offset,
            int limit
    ) {
        currentAuthProvider.getCurrentAuth();

        String normalizedSearch = normalizeSearch(search);
        PathBuilder<Issue> issuePath = new PathBuilder<>(Issue.class, "issue");
        BooleanBuilder predicate = buildIssueListPredicate(issuePath, IssueType.ISSUE, state, normalizedSearch);

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
                .offset(offset)
                .limit(limit)
                .fetch();
        Enrichment enrichment = enrich(paged);

        List<IssueSummaryResponse> items = paged.stream()
                .map(issue -> toIssueSummary(issue, enrichment))
                .toList();

        return new IssueListResponse(
                issueRepository.countByTypeAndState(IssueType.ISSUE, IssueState.OPEN),
                issueRepository.countByTypeAndState(IssueType.ISSUE, IssueState.CLOSED),
                total,
                offset,
                limit,
                items
        );
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssue(int issueNumber) {
        currentAuthProvider.getCurrentAuth();

        Issue issue = issueRepository.findByNumberAndType(issueNumber, IssueType.ISSUE)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Issue #" + issueNumber + "을(를) 찾을 수 없습니다"));

        Enrichment enrichment = enrich(List.of(issue));
        return toIssueResponse(issue, enrichment);
    }

    @Transactional(readOnly = true)
    public ChangeRequestLookupResponse lookupChangeRequests(String search,
            int limit
    ) {
        currentAuthProvider.getCurrentAuth();

        List<ChangeRequestLookupItemResponse> items = changeRequestRepository.findAllByOrderByNumberDesc().stream()
                .filter(changeRequest -> matchesLookupSearch(changeRequest.getNumber(), changeRequest.getTitle(), search))
                .limit(limit)
                .map(changeRequest -> new ChangeRequestLookupItemResponse(
                        changeRequest.getId(),
                        changeRequest.getNumber(),
                        changeRequest.getTitle(),
                        changeRequest.getState(),
                        changeRequest.getCrState()
                ))
                .toList();

        return new ChangeRequestLookupResponse(items);
    }

    @Transactional(readOnly = true)
    public ChangeRequestListResponse listChangeRequests(String search,
            IssueState state,
            CrState crState,
            int offset,
            int limit
    ) {
        currentAuthProvider.getCurrentAuth();

        String normalizedSearch = normalizeSearch(search);
        PathBuilder<ChangeRequest> changeRequestPath = new PathBuilder<>(ChangeRequest.class, "changeRequest");
        BooleanBuilder predicate = buildChangeRequestListPredicate(changeRequestPath, state, crState, normalizedSearch);

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
                .offset(offset)
                .limit(limit)
                .fetch();
        List<Issue> asIssues = paged.stream().map(item -> (Issue) item).toList();
        Enrichment enrichment = enrich(asIssues);

        List<ChangeRequestSummaryResponse> items = paged.stream()
                .map(changeRequest -> toChangeRequestSummary(changeRequest, enrichment))
                .toList();

        return new ChangeRequestListResponse(
                issueRepository.countByTypeAndState(IssueType.CHANGE_REQUEST, IssueState.OPEN),
                issueRepository.countByTypeAndState(IssueType.CHANGE_REQUEST, IssueState.CLOSED),
                total,
                offset,
                limit,
                items
        );
    }

    @Transactional(readOnly = true)
    public ChangeRequestResponse getChangeRequest(int issueNumber) {
        currentAuthProvider.getCurrentAuth();

        ChangeRequest changeRequest = changeRequestRepository.findByNumber(issueNumber)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "ChangeRequest #" + issueNumber + "을(를) 찾을 수 없습니다"
                ));

        Enrichment enrichment = enrich(List.of(changeRequest));
        return toChangeRequestResponse(changeRequest, enrichment);
    }

    @Transactional(readOnly = true)
    public TimelineResponse getIssueTimeline(int issueNumber,
            IssueTargetType targetType
    ) {
        currentAuthProvider.getCurrentAuth();
        IssueType type = toIssueType(targetType);

        Issue issue = issueRepository.findByNumberAndType(issueNumber, type)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "이슈를 찾을 수 없습니다"));

        List<IssueComment> comments = issueCommentRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId());
        List<Activity> activities = activityRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                ActivityTargetType.ISSUE,
                issue.getId()
        );

        List<TimelineItemResponse> merged = new ArrayList<>();
        for (IssueComment comment : comments) {
            merged.add(new TimelineItemResponse(
                    TimelineItemType.COMMENT,
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
            merged.add(new TimelineItemResponse(
                    TimelineItemType.ACTIVITY,
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
        merged.sort(java.util.Comparator.comparing(TimelineItemResponse::createdAt));

        Set<UUID> userIds = collectTimelineUserIds(comments, activities);
        Map<String, IssueUserSummaryResponse> users = toUserSummaryMap(userIds);

        return new TimelineResponse(merged, users);
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
        Map<UUID, Part> partMap = findParts(partIds);

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

    private IssueSummaryResponse toIssueSummary(Issue issue, Enrichment enrichment) {
        return new IssueSummaryResponse(
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

    private ChangeRequestSummaryResponse toChangeRequestSummary(ChangeRequest changeRequest, Enrichment enrichment) {
        return new ChangeRequestSummaryResponse(
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

    private IssueResponse toIssueResponse(Issue issue, Enrichment enrichment) {
        return new IssueResponse(
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

    private ChangeRequestResponse toChangeRequestResponse(ChangeRequest changeRequest, Enrichment enrichment) {
        return new ChangeRequestResponse(
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
                filesOf(changeRequest.getId(), enrichment),
                enrichment.commentCounts().getOrDefault(changeRequest.getId(), 0L).intValue(),
                changeRequest.getCrState(),
                changeRequest.getMergedAt(),
                changeRequest.getMergedBy(),
                linkedIssuesOf(changeRequest.getId(), enrichment)
        );
    }

    private List<LabelBadgeResponse> labelsOf(UUID issueId, Enrichment enrichment) {
        List<LabelBadgeResponse> result = new ArrayList<>();
        for (IssueLabel link : enrichment.labelLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            Label label = enrichment.labels().get(link.getLabelId());
            if (label == null) {
                continue;
            }
            result.add(new LabelBadgeResponse(label.getId(), label.getName(), label.getColor()));
        }
        return result;
    }

    private List<IssueUserSummaryResponse> assigneesOf(UUID issueId, Enrichment enrichment) {
        List<IssueUserSummaryResponse> result = new ArrayList<>();
        for (IssueAssignee link : enrichment.assigneeLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            result.add(toUserSummary(enrichment.userMap().get(link.getUserId())));
        }
        return result.stream().filter(java.util.Objects::nonNull).toList();
    }

    private List<TeamBadgeResponse> assignedTeamsOf(UUID issueId, Enrichment enrichment) {
        List<TeamBadgeResponse> result = new ArrayList<>();
        for (IssueTeamAssignee link : enrichment.teamAssigneeLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            Team team = enrichment.teamMap().get(link.getTeamId());
            if (team != null) {
                result.add(new TeamBadgeResponse(team.getId(), team.getName()));
            }
        }
        return result;
    }

    private List<PartBadgeResponse> partsOf(UUID issueId, Enrichment enrichment) {
        List<PartBadgeResponse> result = new ArrayList<>();
        for (IssuePart link : enrichment.partLinks()) {
            if (!issueId.equals(link.getIssueId())) {
                continue;
            }
            Part part = enrichment.partMap().get(link.getPartId());
            if (part != null) {
                result.add(new PartBadgeResponse(part.getId(), part.getPartNumber(), part.getName()));
            }
        }
        return result;
    }

    private List<FileItemResponse> filesOf(UUID issueId, Enrichment enrichment) {
        return enrichment.files().stream()
                .filter(file -> issueId.equals(file.getOwnerId()))
                .map(this::toFileItem)
                .toList();
    }

    private List<ReviewerSummaryResponse> reviewersOf(UUID changeRequestId, Enrichment enrichment) {
        List<ReviewerSummaryResponse> result = new ArrayList<>();
        for (ChangeRequestReviewer reviewer : enrichment.reviewerLinks()) {
            if (!changeRequestId.equals(reviewer.getChangeRequestId())) {
                continue;
            }
            User user = enrichment.userMap().get(reviewer.getUserId());
            if (user == null) {
                continue;
            }
            result.add(new ReviewerSummaryResponse(
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

    private List<TeamBadgeResponse> reviewerTeamsOf(UUID changeRequestId, Enrichment enrichment) {
        List<TeamBadgeResponse> result = new ArrayList<>();
        for (ChangeRequestTeamReviewer reviewer : enrichment.teamReviewerLinks()) {
            if (!changeRequestId.equals(reviewer.getChangeRequestId())) {
                continue;
            }
            Team team = enrichment.teamMap().get(reviewer.getTeamId());
            if (team != null) {
                result.add(new TeamBadgeResponse(team.getId(), team.getName()));
            }
        }
        return result;
    }

    private List<LinkedIssueBadgeResponse> linkedIssuesOf(UUID changeRequestId, Enrichment enrichment) {
        List<LinkedIssueBadgeResponse> result = new ArrayList<>();
        for (ChangeRequestIssue link : enrichment.linksByCrId().getOrDefault(changeRequestId, List.of())) {
            Issue issue = enrichment.linkedIssueMap().get(link.getIssueId());
            if (issue == null) {
                continue;
            }
            result.add(new LinkedIssueBadgeResponse(issue.getId(), issue.getNumber(), issue.getTitle(), issue.getState()));
        }
        return result;
    }

    private List<LinkedChangeRequestBadgeResponse> linkedChangesOf(UUID issueId, Enrichment enrichment) {
        List<LinkedChangeRequestBadgeResponse> result = new ArrayList<>();
        for (ChangeRequestIssue link : enrichment.linksByIssueId().getOrDefault(issueId, List.of())) {
            ChangeRequest changeRequest = enrichment.linkedCrMap().get(link.getChangeRequestId());
            if (changeRequest == null) {
                continue;
            }
            result.add(new LinkedChangeRequestBadgeResponse(
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

    private Map<String, IssueUserSummaryResponse> toUserSummaryMap(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<String, IssueUserSummaryResponse> map = new LinkedHashMap<>();
        for (User user : userRepository.findAllByIdInOrderByFullName(userIds)) {
            map.put(user.getId().toString(), toUserSummary(user));
        }
        return map;
    }

    private IssueUserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new IssueUserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private FileItemResponse toFileItem(File file) {
        return new FileItemResponse(
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
        userRepository.findAllByIdInOrderByFullName(userIds).forEach(user -> map.put(user.getId(), user));
        return map;
    }

    private Map<UUID, Part> findParts(Set<UUID> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Part> map = new HashMap<>();
        partRepository.findAllById(partIds).forEach(part -> map.put(part.getId(), part));
        return map;
    }

    private Map<UUID, Long> countComments(List<UUID> issueIds) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : issueCommentRepository.countByIssueIds(issueIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
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
            Map<UUID, Part> partMap,
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
