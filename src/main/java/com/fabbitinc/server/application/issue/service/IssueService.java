package com.fabbitinc.server.application.issue.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.support.MentionExtractor;
import com.fabbitinc.server.application.issue.support.TipTapValidator;
import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
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
import com.fabbitinc.server.domain.issue.model.IssueNumberSequence;
import com.fabbitinc.server.domain.issue.model.IssuePart;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueTeamAssignee;
import com.fabbitinc.server.domain.issue.model.IssueType;
import com.fabbitinc.server.domain.issue.model.ReviewStatus;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestIssueRepository;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestRepository;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestReviewerRepository;
import com.fabbitinc.server.domain.issue.repository.ChangeRequestTeamReviewerRepository;
import com.fabbitinc.server.domain.issue.repository.IssueAssigneeRepository;
import com.fabbitinc.server.domain.issue.repository.IssueCommentRepository;
import com.fabbitinc.server.domain.issue.repository.IssueLabelRepository;
import com.fabbitinc.server.domain.issue.repository.IssueNumberSequenceRepository;
import com.fabbitinc.server.domain.issue.repository.IssuePartRepository;
import com.fabbitinc.server.domain.issue.repository.IssueRepository;
import com.fabbitinc.server.domain.issue.repository.IssueTeamAssigneeRepository;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import com.fabbitinc.server.domain.notification.model.Notification;
import com.fabbitinc.server.domain.notification.model.NotificationSourceIssueType;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import com.fabbitinc.server.domain.notification.repository.NotificationRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
public class IssueService {

    private static final String OWNER_TYPE_ISSUE = "issue";
    private static final UUID ISSUE_NUMBER_SEQUENCE_ID = UUID.fromString("89d98a7b-6b53-4e63-a02a-73f66f606703");

    private static final ActivityAction ACTION_ISSUE_STATE_CHANGED = ActivityAction.ISSUE_STATE_CHANGED;
    private static final ActivityAction ACTION_CR_STATE_CHANGED = ActivityAction.CR_STATE_CHANGED;
    private static final ActivityAction ACTION_ASSIGNEE_CHANGED = ActivityAction.ISSUE_ASSIGNEE_CHANGED;
    private static final ActivityAction ACTION_REVIEWER_CHANGED = ActivityAction.ISSUE_REVIEWER_CHANGED;
    private static final ActivityAction ACTION_LABEL_CHANGED = ActivityAction.ISSUE_LABEL_CHANGED;
    private static final ActivityAction ACTION_PART_CHANGED = ActivityAction.ISSUE_PART_CHANGED;
    private static final ActivityAction ACTION_FILE_ATTACHED = ActivityAction.ISSUE_FILE_ATTACHED;
    private static final ActivityAction ACTION_FILE_DETACHED = ActivityAction.ISSUE_FILE_DETACHED;
    private static final ActivityAction ACTION_CR_ISSUE_CHANGED = ActivityAction.CR_ISSUE_CHANGED;
    private static final ActivityAction ACTION_ISSUE_CR_CHANGED = ActivityAction.ISSUE_CR_CHANGED;
    private static final ActivityAction ACTION_ISSUE_MENTIONED = ActivityAction.ISSUE_MENTIONED;

    private final IssueRepository issueRepository;
    private final IssueNumberSequenceRepository issueNumberSequenceRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final IssueAssigneeRepository issueAssigneeRepository;
    private final IssueTeamAssigneeRepository issueTeamAssigneeRepository;
    private final IssuePartRepository issuePartRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final ChangeRequestReviewerRepository changeRequestReviewerRepository;
    private final ChangeRequestTeamReviewerRepository changeRequestTeamReviewerRepository;
    private final ChangeRequestIssueRepository changeRequestIssueRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final PartRepository partRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final NotificationRepository notificationRepository;
    private final TipTapValidator tipTapValidator;
    private final MentionExtractor mentionExtractor;
    private final ObjectMapper objectMapper;

    public Issue getIssueByNumberOrThrow(int issueNumber) {
        return issueRepository.findByNumberAndType(issueNumber, IssueType.ISSUE)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "이슈를 찾을 수 없습니다"));
    }

    public ChangeRequest getChangeRequestByNumberOrThrow(int issueNumber) {
        return changeRequestRepository.findByNumber(issueNumber)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "변경 요청을 찾을 수 없습니다"));
    }

    public Issue getIssueOrThrow(UUID issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Issue '" + issueId + "'을(를) 찾을 수 없습니다"));
    }

    public ChangeRequest getChangeRequestOrThrow(UUID issueId) {
        return changeRequestRepository.findById(issueId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "ChangeRequest '" + issueId + "'을(를) 찾을 수 없습니다"));
    }

    public Issue createIssue(UUID actorId, String title, JsonNode body) {
        tipTapValidator.validateDocument(body);
        int nextNumber = allocateIssueNumber();

        Issue issue = Issue.create(nextNumber, title, toBodyString(body), actorId);
        issueRepository.save(issue);

        registerMentions(issue, actorId, body, null, false);
        return issue;
    }

    public ChangeRequest createChangeRequest(UUID actorId, String title, JsonNode body) {
        tipTapValidator.validateDocument(body);
        int nextNumber = allocateIssueNumber();

        ChangeRequest changeRequest = ChangeRequest.create(nextNumber, title, toBodyString(body), actorId);
        changeRequestRepository.save(changeRequest);

        registerMentions(changeRequest, actorId, body, null, false);
        return changeRequest;
    }

    private int allocateIssueNumber() {
        IssueNumberSequence sequence = issueNumberSequenceRepository.findByIdForUpdate(ISSUE_NUMBER_SEQUENCE_ID)
                .orElseGet(this::initializeIssueNumberSequence);
        return sequence.allocateNextNumber();
    }

    private IssueNumberSequence initializeIssueNumberSequence() {
        int nextNumber = issueRepository.findTopByOrderByNumberDesc()
                .map(issue -> issue.getNumber() + 1)
                .orElse(1);

        try {
            return issueNumberSequenceRepository.saveAndFlush(
                    IssueNumberSequence.initialize(ISSUE_NUMBER_SEQUENCE_ID, nextNumber)
            );
        } catch (DataIntegrityViolationException ex) {
            return issueNumberSequenceRepository.findByIdForUpdate(ISSUE_NUMBER_SEQUENCE_ID)
                    .orElseThrow(() -> new AppException(
                            ErrorCode.INTERNAL_SERVER_ERROR,
                            "이슈 번호 시퀀스를 초기화할 수 없습니다"
                    ));
        }
    }

    public Issue updateIssue(UUID actorId, Issue issue, String title, JsonNode body) {
        if (issue.getState() == IssueState.CLOSED) {
            throw new AppException(ErrorCode.INVALID_STATE, "닫힌 이슈는 수정할 수 없습니다");
        }

        JsonNode oldBody = body == null ? null : parseJson(issue.getBody());
        if (title != null) {
            issue.updateTitle(title, actorId);
        }
        if (body != null) {
            tipTapValidator.validateDocument(body);
            issue.updateBody(toBodyString(body), actorId);
            registerMentions(issue, actorId, body, oldBody, false);
        }
        return issue;
    }

    public ChangeRequest updateChangeRequest(UUID actorId, ChangeRequest changeRequest, String title, JsonNode body) {
        if (changeRequest.getCrState() == CrState.MERGED || changeRequest.getCrState() == CrState.CLOSED) {
            throw new AppException(
                    ErrorCode.INVALID_STATE,
                    "'" + changeRequest.getCrState() + "' 상태에서는 수정할 수 없습니다"
            );
        }
        updateIssue(actorId, changeRequest, title, body);
        return changeRequest;
    }

    public Issue closeIssue(UUID actorId, Issue issue) {
        String oldState = issue.getState().name();
        issue.close(Instant.now(), actorId);
        addStateActivity(issue.getId(), actorId, ACTION_ISSUE_STATE_CHANGED, oldState, issue.getState().name());
        return issue;
    }

    public Issue reopenIssue(UUID actorId, Issue issue) {
        String oldState = issue.getState().name();
        issue.reopen(actorId);
        addStateActivity(issue.getId(), actorId, ACTION_ISSUE_STATE_CHANGED, oldState, issue.getState().name());
        return issue;
    }

    public ChangeRequest submitChangeRequest(UUID actorId, ChangeRequest changeRequest) {
        String oldState = changeRequest.getCrState().name();
        try {
            changeRequest.submit(actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(changeRequest.getId(), actorId, ACTION_CR_STATE_CHANGED, oldState, changeRequest.getCrState().name());
        return changeRequest;
    }

    public ChangeRequest mergeChangeRequest(UUID actorId, ChangeRequest changeRequest) {
        String oldState = changeRequest.getCrState().name();
        try {
            changeRequest.merge(Instant.now(), actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(changeRequest.getId(), actorId, ACTION_CR_STATE_CHANGED, oldState, changeRequest.getCrState().name());
        closeLinkedOpenIssuesIfResolved(actorId, changeRequest.getId());
        return changeRequest;
    }

    public ChangeRequest closeChangeRequest(UUID actorId, ChangeRequest changeRequest) {
        String oldState = changeRequest.getCrState().name();
        try {
            changeRequest.closeCr(Instant.now(), actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(changeRequest.getId(), actorId, ACTION_CR_STATE_CHANGED, oldState, changeRequest.getCrState().name());
        return changeRequest;
    }

    public ChangeRequest reopenChangeRequest(UUID actorId, ChangeRequest changeRequest) {
        String oldState = changeRequest.getCrState().name();
        try {
            changeRequest.reopenCr(actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(changeRequest.getId(), actorId, ACTION_CR_STATE_CHANGED, oldState, changeRequest.getCrState().name());
        return changeRequest;
    }

    public DiffResult syncAssignees(UUID actorId, UUID issueId, List<UUID> userIds, boolean emitActivity) {
        Set<UUID> current = issueAssigneeRepository.findByIssueId(issueId).stream()
                .map(IssueAssignee::getUserId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> desired = new LinkedHashSet<>(userIds);

        Set<UUID> assignedTeamIds = issueTeamAssigneeRepository.findByIssueId(issueId).stream()
                .map(IssueTeamAssignee::getTeamId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> coveredByTeam = teamMemberRepository.findByTeam_IdIn(assignedTeamIds).stream()
                .map(TeamMember::getUserId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);
        toAdd.removeAll(coveredByTeam);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            issueAssigneeRepository.deleteByIssueIdAndUserIdIn(issueId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            Issue issue = getIssueOrThrow(issueId);
            List<IssueAssignee> adds = toAdd.stream().map(issue::assignUser).toList();
            issueAssigneeRepository.saveAll(adds);
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Map<UUID, User> users = findUsers(Set.copyOf(union(toAdd, toRemove)));
            addDiffActivity(
                    issueId,
                    actorId,
                    ACTION_ASSIGNEE_CHANGED,
                    toAdd.stream().map(userId -> toUserRef(userId, users.get(userId))).toList(),
                    toRemove.stream().map(userId -> toUserRef(userId, users.get(userId))).toList()
            );
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncTeamAssignees(UUID issueId, List<UUID> teamIds) {
        Set<UUID> current = issueTeamAssigneeRepository.findByIssueId(issueId).stream()
                .map(IssueTeamAssignee::getTeamId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(teamIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            issueTeamAssigneeRepository.deleteByIssueIdAndTeamIdIn(issueId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            Issue issue = getIssueOrThrow(issueId);
            List<IssueTeamAssignee> adds = toAdd.stream().map(issue::assignTeam).toList();
            issueTeamAssigneeRepository.saveAll(adds);

            Set<UUID> overlapUsers = teamMemberRepository.findByTeam_IdIn(toAdd).stream()
                    .map(TeamMember::getUserId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!overlapUsers.isEmpty()) {
                issueAssigneeRepository.deleteByIssueIdAndUserIdIn(issueId, overlapUsers);
            }
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncReviewers(UUID actorId, UUID changeRequestId, List<UUID> userIds, boolean emitActivity) {
        Set<UUID> current = changeRequestReviewerRepository.findByChangeRequestId(changeRequestId).stream()
                .map(ChangeRequestReviewer::getUserId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(userIds);

        Set<UUID> reviewerTeamIds = changeRequestTeamReviewerRepository.findByChangeRequestId(changeRequestId).stream()
                .map(ChangeRequestTeamReviewer::getTeamId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> coveredByTeams = teamMemberRepository.findByTeam_IdIn(reviewerTeamIds).stream()
                .map(TeamMember::getUserId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);
        toAdd.removeAll(coveredByTeams);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            changeRequestReviewerRepository.deleteByChangeRequestIdAndUserIdIn(changeRequestId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            ChangeRequest changeRequest = getChangeRequestOrThrow(changeRequestId);
            List<ChangeRequestReviewer> adds = toAdd.stream().map(changeRequest::assignReviewer).toList();
            changeRequestReviewerRepository.saveAll(adds);
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Map<UUID, User> users = findUsers(Set.copyOf(union(toAdd, toRemove)));
            addDiffActivity(
                    changeRequestId,
                    actorId,
                    ACTION_REVIEWER_CHANGED,
                    toAdd.stream().map(userId -> toUserRef(userId, users.get(userId))).toList(),
                    toRemove.stream().map(userId -> toUserRef(userId, users.get(userId))).toList()
            );
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncTeamReviewers(UUID changeRequestId, List<UUID> teamIds) {
        Set<UUID> current = changeRequestTeamReviewerRepository.findByChangeRequestId(changeRequestId).stream()
                .map(ChangeRequestTeamReviewer::getTeamId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(teamIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            changeRequestTeamReviewerRepository.deleteByChangeRequestIdAndTeamIdIn(changeRequestId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            ChangeRequest changeRequest = getChangeRequestOrThrow(changeRequestId);
            List<ChangeRequestTeamReviewer> adds = toAdd.stream().map(changeRequest::assignTeamReviewer).toList();
            changeRequestTeamReviewerRepository.saveAll(adds);

            Set<UUID> overlapUsers = teamMemberRepository.findByTeam_IdIn(toAdd).stream()
                    .map(TeamMember::getUserId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!overlapUsers.isEmpty()) {
                changeRequestReviewerRepository.deleteByChangeRequestIdAndUserIdIn(changeRequestId, overlapUsers);
            }
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncLabels(UUID actorId, UUID issueId, List<UUID> labelIds, boolean emitActivity) {
        validateLabels(labelIds);

        Set<UUID> current = issueLabelRepository.findByIssueId(issueId).stream()
                .map(IssueLabel::getLabelId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(labelIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            issueLabelRepository.deleteByIssueIdAndLabelIdIn(issueId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            Issue issue = getIssueOrThrow(issueId);
            List<IssueLabel> adds = toAdd.stream().map(issue::linkLabel).toList();
            issueLabelRepository.saveAll(adds);
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Map<UUID, Label> labels = findLabels(Set.copyOf(union(toAdd, toRemove)));
            addDiffActivity(
                    issueId,
                    actorId,
                    ACTION_LABEL_CHANGED,
                    toAdd.stream().map(labelId -> toLabelRef(labelId, labels.get(labelId))).toList(),
                    toRemove.stream().map(labelId -> toLabelRef(labelId, labels.get(labelId))).toList()
            );
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncParts(UUID actorId, UUID issueId, List<UUID> partIds, boolean emitActivity) {
        Set<UUID> current = issuePartRepository.findByIssueId(issueId).stream()
                .map(IssuePart::getPartId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(partIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            issuePartRepository.deleteByIssueIdAndPartIdIn(issueId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            Issue issue = getIssueOrThrow(issueId);
            List<IssuePart> adds = toAdd.stream().map(issue::linkPart).toList();
            issuePartRepository.saveAll(adds);
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Map<UUID, Part> parts = findParts(Set.copyOf(union(toAdd, toRemove)));
            addDiffActivity(
                    issueId,
                    actorId,
                    ACTION_PART_CHANGED,
                    toAdd.stream().map(partId -> toPartRef(partId, parts.get(partId))).toList(),
                    toRemove.stream().map(partId -> toPartRef(partId, parts.get(partId))).toList()
            );
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncIssues(UUID actorId, UUID changeRequestId, List<UUID> issueIds, boolean emitActivity) {
        validateIssueIds(issueIds);

        Set<UUID> current = changeRequestIssueRepository.findByChangeRequestId(changeRequestId).stream()
                .map(ChangeRequestIssue::getIssueId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(issueIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            changeRequestIssueRepository.deleteByChangeRequestIdAndIssueIdIn(changeRequestId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            ChangeRequest changeRequest = getChangeRequestOrThrow(changeRequestId);
            List<ChangeRequestIssue> adds = toAdd.stream().map(changeRequest::linkIssue).toList();
            changeRequestIssueRepository.saveAll(adds);
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            ChangeRequest changeRequest = getChangeRequestOrThrow(changeRequestId);
            Map<UUID, Issue> issues = findIssues(Set.copyOf(union(toAdd, toRemove)));

            List<Map<String, Object>> addedIssueRefs = toAdd.stream().map(issueId -> toIssueRef(issues.get(issueId))).toList();
            List<Map<String, Object>> removedIssueRefs = toRemove.stream().map(issueId -> toIssueRef(issues.get(issueId))).toList();

            addDiffActivity(changeRequestId, actorId, ACTION_CR_ISSUE_CHANGED, addedIssueRefs, removedIssueRefs);

            Map<String, Object> crRef = toCrRef(changeRequest);
            for (UUID addedIssueId : toAdd) {
                addDiffActivity(
                        addedIssueId,
                        actorId,
                        ACTION_CR_ISSUE_CHANGED,
                        List.of(crRef),
                        List.of()
                );
            }
            for (UUID removedIssueId : toRemove) {
                addDiffActivity(
                        removedIssueId,
                        actorId,
                        ACTION_CR_ISSUE_CHANGED,
                        List.of(),
                        List.of(crRef)
                );
            }
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncChanges(UUID actorId, UUID issueId, List<UUID> changeRequestIds, boolean emitActivity) {
        validateChangeRequestIds(changeRequestIds);

        Set<UUID> current = changeRequestIssueRepository.findByIssueId(issueId).stream()
                .map(ChangeRequestIssue::getChangeRequestId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(changeRequestIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            changeRequestIssueRepository.deleteByIssueIdAndChangeRequestIdIn(issueId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            Map<UUID, ChangeRequest> changeRequests = findChangeRequests(toAdd);
            List<ChangeRequestIssue> adds = toAdd.stream()
                    .map(changeId -> changeRequests.get(changeId).linkIssue(issueId))
                    .toList();
            changeRequestIssueRepository.saveAll(adds);
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Issue issue = getIssueOrThrow(issueId);
            Map<UUID, ChangeRequest> crs = findChangeRequests(Set.copyOf(union(toAdd, toRemove)));

            List<Map<String, Object>> addedCrRefs = toAdd.stream().map(changeId -> toCrRef(crs.get(changeId))).toList();
            List<Map<String, Object>> removedCrRefs = toRemove.stream().map(changeId -> toCrRef(crs.get(changeId))).toList();

            addDiffActivity(issueId, actorId, ACTION_ISSUE_CR_CHANGED, addedCrRefs, removedCrRefs);

            Map<String, Object> issueRef = toIssueRef(issue);
            for (UUID addedCrId : toAdd) {
                addDiffActivity(addedCrId, actorId, ACTION_ISSUE_CR_CHANGED, List.of(issueRef), List.of());
            }
            for (UUID removedCrId : toRemove) {
                addDiffActivity(removedCrId, actorId, ACTION_ISSUE_CR_CHANGED, List.of(), List.of(issueRef));
            }
        }

        return new DiffResult(toAdd, toRemove);
    }

    public ChangeRequestReviewer submitReview(UUID actorId, UUID changeRequestId, ReviewStatus status) {
        if (status == null || status == ReviewStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "리뷰 상태는 APPROVED 또는 REJECTED만 허용됩니다");
        }
        ChangeRequestReviewer reviewer = changeRequestReviewerRepository
                .findByChangeRequestIdAndUserId(changeRequestId, actorId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "해당 변경 요청의 검토자가 아닙니다"));

        reviewer.submit(status, Instant.now());
        return reviewer;
    }

    public IssueComment createComment(UUID actorId, UUID issueId, JsonNode body) {
        tipTapValidator.validateDocument(body);
        Issue issue = getIssueOrThrow(issueId);

        IssueComment comment = issue.writeComment(toBodyString(body), actorId);
        issueCommentRepository.save(comment);

        registerMentions(issue, actorId, body, null, true);
        return comment;
    }

    public IssueComment updateComment(UUID actorId, UUID issueId, UUID commentId, JsonNode body) {
        tipTapValidator.validateDocument(body);
        Issue issue = getIssueOrThrow(issueId);

        IssueComment comment = issueCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다"));

        if (!comment.getIssueId().equals(issueId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "해당 이슈의 댓글이 아닙니다");
        }
        if (!comment.getCreatedBy().equals(actorId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "본인이 작성한 댓글만 수정할 수 있습니다");
        }

        JsonNode oldBody = parseJson(comment.getBody());
        comment.updateBody(toBodyString(body), actorId);
        registerMentions(issue, actorId, body, oldBody, true);

        return comment;
    }

    public void deleteComment(UUID actorId, UUID issueId, UUID commentId) {
        getIssueOrThrow(issueId);

        IssueComment comment = issueCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다"));

        if (!comment.getIssueId().equals(issueId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "해당 이슈의 댓글이 아닙니다");
        }
        if (!comment.getCreatedBy().equals(actorId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "본인이 작성한 댓글만 삭제할 수 있습니다");
        }

        issueCommentRepository.delete(comment);
    }

    public List<File> attachFiles(UUID actorId, UUID issueId, List<File> files) {
        return attachFiles(actorId, issueId, files, true);
    }

    public List<File> attachFiles(UUID actorId, UUID issueId, List<File> files, boolean emitActivity) {
        getIssueOrThrow(issueId);
        if (files.isEmpty()) {
            return List.of();
        }

        for (File file : files) {
            file.assignOwner(OWNER_TYPE_ISSUE, issueId);
        }

        if (emitActivity) {
            List<Map<String, Object>> addedRefs = files.stream().map(this::toFileRef).toList();
            addDiffActivity(issueId, actorId, ACTION_FILE_ATTACHED, addedRefs, List.of());
        }
        return files;
    }

    public void detachFile(UUID actorId, UUID issueId, UUID fileId) {
        getIssueOrThrow(issueId);

        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(fileId, OWNER_TYPE_ISSUE, issueId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "해당 이슈에 연결된 파일을 찾을 수 없습니다"));

        String fileName = file.getOriginalName();
        file.softDelete();

        Map<String, Object> removed = new LinkedHashMap<>();
        removed.put("id", fileId.toString());
        removed.put("type", "file");
        removed.put("label", fileName == null ? "(알 수 없음)" : fileName);

        addDiffActivity(issueId, actorId, ACTION_FILE_DETACHED, List.of(), List.of(removed));
    }

    private void closeLinkedOpenIssuesIfResolved(UUID actorId, UUID changeRequestId) {
        List<UUID> linkedIssueIds = changeRequestIssueRepository.findByChangeRequestId(changeRequestId).stream()
                .map(ChangeRequestIssue::getIssueId)
                .toList();

        for (UUID linkedIssueId : linkedIssueIds) {
            Issue linked = issueRepository.findByIdAndType(linkedIssueId, IssueType.ISSUE).orElse(null);
            if (linked == null || linked.getState() != IssueState.OPEN) {
                continue;
            }

            if (!hasUnresolvedLinkedChangeRequests(linkedIssueId)) {
                String oldState = linked.getState().name();
                linked.close(Instant.now(), actorId);
                addStateActivity(linked.getId(), actorId, ACTION_ISSUE_STATE_CHANGED, oldState, linked.getState().name());
            }
        }
    }

    private boolean hasUnresolvedLinkedChangeRequests(UUID issueId) {
        List<UUID> changeRequestIds = changeRequestIssueRepository.findByIssueId(issueId).stream()
                .map(ChangeRequestIssue::getChangeRequestId)
                .toList();
        if (changeRequestIds.isEmpty()) {
            return false;
        }

        for (ChangeRequest changeRequest : changeRequestRepository.findAllById(changeRequestIds)) {
            if (changeRequest.getCrState() != CrState.MERGED && changeRequest.getCrState() != CrState.CLOSED) {
                return true;
            }
        }
        return false;
    }

    private void validateLabels(Collection<UUID> labelIds) {
        if (labelIds.isEmpty()) {
            return;
        }
        Set<UUID> foundIds = labelRepository.findAllById(labelIds).stream()
                .map(Label::getId)
                .collect(java.util.stream.Collectors.toSet());
        for (UUID labelId : labelIds) {
            if (!foundIds.contains(labelId)) {
                throw new AppException(ErrorCode.NOT_FOUND, "Label '" + labelId + "'을(를) 찾을 수 없습니다");
            }
        }
    }

    private void validateIssueIds(Collection<UUID> issueIds) {
        for (UUID issueId : issueIds) {
            if (issueRepository.findByIdAndType(issueId, IssueType.ISSUE).isEmpty()) {
                throw new AppException(ErrorCode.NOT_FOUND, "Issue '" + issueId + "'을(를) 찾을 수 없습니다");
            }
        }
    }

    private void validateChangeRequestIds(Collection<UUID> changeRequestIds) {
        for (UUID changeRequestId : changeRequestIds) {
            if (changeRequestRepository.findById(changeRequestId).isEmpty()) {
                throw new AppException(ErrorCode.NOT_FOUND, "ChangeRequest '" + changeRequestId + "'을(를) 찾을 수 없습니다");
            }
        }
    }

    private void addStateActivity(UUID issueId, UUID actorId, ActivityAction action, String oldState, String newState) {
        Map<String, Object> detail = Map.of(
                "changes",
                Map.of(
                        "state",
                        Map.of("old", oldState, "new", newState)
                )
        );
        addActivity(issueId, actorId, action, detail);
    }

    private void addDiffActivity(
            UUID targetIssueId,
            UUID actorId,
            ActivityAction action,
            List<Map<String, Object>> added,
            List<Map<String, Object>> removed
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("added", added);
        detail.put("removed", removed);
        addActivity(targetIssueId, actorId, action, detail);
    }

    private void addActivity(UUID targetIssueId, UUID actorId, ActivityAction action, Object detail) {
        Activity activity = Activity.create(
                ActivityTargetType.ISSUE,
                targetIssueId,
                action.value(),
                actorId,
                toJsonString(detail)
        );
        activityRepository.save(activity);
    }

    private void registerMentions(
            Issue issue,
            UUID actorId,
            JsonNode newBody,
            JsonNode oldBody,
            boolean isComment
    ) {
        MentionExtractor.MentionSet newMentions = mentionExtractor.extract(newBody);
        MentionExtractor.MentionSet oldMentions = mentionExtractor.extract(oldBody);

        Set<UUID> addedIssueMentions = new LinkedHashSet<>(newMentions.issueIds());
        addedIssueMentions.removeAll(oldMentions.issueIds());
        addedIssueMentions.remove(issue.getId());

        NotificationSourceIssueType sourceIssueType = issue.getType() == IssueType.CHANGE_REQUEST
                ? NotificationSourceIssueType.CHANGE_REQUEST
                : NotificationSourceIssueType.ISSUE;
        for (UUID targetIssueId : addedIssueMentions) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("id", issue.getId().toString());
            ref.put("type", sourceIssueType);
            ref.put("label", "#" + issue.getNumber() + " " + issue.getTitle());
            ref.put("meta", Map.of("number", issue.getNumber(), "is_comment", isComment));

            addActivity(
                    targetIssueId,
                    actorId,
                    ACTION_ISSUE_MENTIONED,
                    Map.of("refs", List.of(ref))
            );
        }

        Set<UUID> addedUserMentions = new LinkedHashSet<>(newMentions.userIds());
        addedUserMentions.removeAll(oldMentions.userIds());
        addedUserMentions.remove(actorId);

        for (UUID userId : addedUserMentions) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("source_issue_id", issue.getId().toString());
            payload.put("source_number", issue.getNumber());
            payload.put("source_title", issue.getTitle());
            payload.put("source_issue_type", sourceIssueType);
            payload.put("is_comment", isComment);

            notificationRepository.save(Notification.create(
                    userId,
                    NotificationType.MENTION,
                    actorId,
                    toJsonString(payload)
            ));
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

    private String toBodyString(JsonNode body) {
        if (body == null || body.isNull()) {
            return null;
        }
        return toJsonString(body);
    }

    private String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "JSON 직렬화에 실패했습니다");
        }
    }

    private Map<UUID, User> findUsers(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, User> map = new HashMap<>();
        for (User user : userRepository.findByIdInOrderByFullNameAsc(userIds)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private Map<UUID, Label> findLabels(Set<UUID> labelIds) {
        if (labelIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Label> map = new HashMap<>();
        for (Label label : labelRepository.findAllById(labelIds)) {
            map.put(label.getId(), label);
        }
        return map;
    }

    private Map<UUID, Part> findParts(Set<UUID> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Part> map = new HashMap<>();
        for (Part part : partRepository.findAllById(partIds)) {
            map.put(part.getId(), part);
        }
        return map;
    }

    private Map<UUID, Issue> findIssues(Set<UUID> issueIds) {
        if (issueIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Issue> map = new HashMap<>();
        for (Issue issue : issueRepository.findAllById(issueIds)) {
            map.put(issue.getId(), issue);
        }
        return map;
    }

    private Map<UUID, ChangeRequest> findChangeRequests(Set<UUID> changeRequestIds) {
        if (changeRequestIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ChangeRequest> map = new HashMap<>();
        for (ChangeRequest changeRequest : changeRequestRepository.findAllById(changeRequestIds)) {
            map.put(changeRequest.getId(), changeRequest);
        }
        return map;
    }

    private Set<UUID> union(Set<UUID> a, Set<UUID> b) {
        Set<UUID> union = new LinkedHashSet<>(a);
        union.addAll(b);
        return union;
    }

    private Map<String, Object> toUserRef(UUID userId, User user) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", userId.toString());
        ref.put("type", "user");
        ref.put("label", user == null ? "(알 수 없음)" : user.getFullName());
        return ref;
    }

    private Map<String, Object> toLabelRef(UUID labelId, Label label) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", labelId.toString());
        ref.put("type", "label");
        ref.put("label", label == null ? "(삭제됨)" : label.getName());

        if (label != null) {
            ref.put("meta", Map.of("color", label.getColor()));
        } else {
            ref.put("meta", Map.of("color", "#888888"));
        }
        return ref;
    }

    private Map<String, Object> toPartRef(UUID partId, Part part) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", partId.toString());
        ref.put("type", "part");
        ref.put("label", part == null ? "(알 수 없음)" : part.getPartNumber());
        return ref;
    }

    private Map<String, Object> toIssueRef(Issue issue) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", issue == null ? "" : issue.getId().toString());
        ref.put("type", issue == null ? "issue" : issue.getType().name());
        ref.put("label", issue == null ? "(알 수 없음)" : "#" + issue.getNumber() + " " + issue.getTitle());
        ref.put("meta", Map.of("number", issue == null ? 0 : issue.getNumber()));
        return ref;
    }

    private Map<String, Object> toCrRef(ChangeRequest changeRequest) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", changeRequest == null ? "" : changeRequest.getId().toString());
        ref.put("type", "cr");
        ref.put("label", changeRequest == null ? "(알 수 없음)" : "#" + changeRequest.getNumber() + " " + changeRequest.getTitle());
        ref.put("meta", Map.of("number", changeRequest == null ? 0 : changeRequest.getNumber()));
        return ref;
    }

    private Map<String, Object> toFileRef(File file) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", file.getId().toString());
        ref.put("type", "file");
        ref.put("label", file.getOriginalName());
        return ref;
    }

    public record DiffResult(
            Set<UUID> added,
            Set<UUID> removed
    ) {
    }
}
