package com.fabbitinc.server.application.issue.service;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.event.IssueUsersMentionedEvent;
import com.fabbitinc.server.application.issue.support.MentionExtractor;
import com.fabbitinc.server.application.issue.support.TipTapValidator;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.api.EngineeringChangePartRevisionSnapshot;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
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
import com.fabbitinc.server.domain.issue.model.IssueNumberSequence;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.ReviewStatus;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeReviewerRepository;
import com.fabbitinc.server.domain.issue.repository.EngineeringChangeTeamReviewerRepository;
import com.fabbitinc.server.domain.issue.repository.IssueNumberSequenceRepository;
import com.fabbitinc.server.domain.issue.repository.IssueRepository;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EngineeringChangeService {

    private static final String OWNER_TYPE_ENGINEERING_CHANGE = "engineering_change";
    private static final UUID ISSUE_NUMBER_SEQUENCE_ID = UUID.fromString("89d98a7b-6b53-4e63-a02a-73f66f606703");

    private static final ActivityAction ACTION_ISSUE_STATE_CHANGED = ActivityAction.ISSUE_STATE_CHANGED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_STATE_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_STATE_CHANGED;
    private static final ActivityAction ACTION_REVIEWER_CHANGED = ActivityAction.ISSUE_REVIEWER_CHANGED;
    private static final ActivityAction ACTION_FILE_ATTACHED = ActivityAction.ISSUE_FILE_ATTACHED;
    private static final ActivityAction ACTION_FILE_DETACHED = ActivityAction.ISSUE_FILE_DETACHED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_ISSUE_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_ISSUE_CHANGED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_PART_REVISION_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_PART_REVISION_CHANGED;
    private static final ActivityAction ACTION_ISSUE_ENGINEERING_CHANGE_CHANGED =
            ActivityAction.ISSUE_ENGINEERING_CHANGE_CHANGED;
    private static final ActivityAction ACTION_ISSUE_MENTIONED = ActivityAction.ISSUE_MENTIONED;

    private final IssueRepository issueRepository;
    private final IssueNumberSequenceRepository issueNumberSequenceRepository;
    private final EngineeringChangeRepository engineeringChangeRepository;
    private final EngineeringChangeReviewerRepository engineeringChangeReviewerRepository;
    private final EngineeringChangeTeamReviewerRepository engineeringChangeTeamReviewerRepository;
    private final EngineeringChangeIssueLinkRepository engineeringChangeIssueRepository;
    private final EngineeringChangeCommentRepository engineeringChangeCommentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrganizationApi organizationApi;
    private final TipTapValidator tipTapValidator;
    private final MentionExtractor mentionExtractor;
    private final ObjectMapper objectMapper;

    public Issue getIssueByNumberOrThrow(int issueNumber) {
        return issueRepository.findByNumber(issueNumber)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "이슈를 찾을 수 없습니다"));
    }

    public EngineeringChange getEngineeringChangeByNumberOrThrow(int issueNumber) {
        return engineeringChangeRepository.findByNumber(issueNumber)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "변경관리를 찾을 수 없습니다"));
    }

    public EngineeringChange getEngineeringChangeOrThrow(UUID engineeringChangeId) {
        return engineeringChangeRepository.findById(engineeringChangeId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "EngineeringChange '" + engineeringChangeId + "'을(를) 찾을 수 없습니다"
                ));
    }

    public EngineeringChange createEngineeringChange(UUID actorId, String title, JsonNode body) {
        tipTapValidator.validateDocument(body);
        EngineeringChange engineeringChange = EngineeringChange.create(
                allocateIssueNumber(),
                title,
                toBodyString(body),
                actorId
        );
        engineeringChangeRepository.save(engineeringChange);

        registerMentions(
                engineeringChange.getId(),
                actorId,
                body,
                null,
                false,
                engineeringChange.getNumber(),
                engineeringChange.getTitle(),
                "engineering_change"
        );
        return engineeringChange;
    }

    public EngineeringChange updateEngineeringChange(
            UUID actorId,
            EngineeringChange engineeringChange,
            String title,
            JsonNode body
    ) {
        if (engineeringChange.getEngineeringChangeState() == EngineeringChangeState.MERGED
                || engineeringChange.getEngineeringChangeState() == EngineeringChangeState.CLOSED) {
            throw new AppException(
                    ErrorCode.INVALID_STATE,
                    "'" + engineeringChange.getEngineeringChangeState() + "' 상태에서는 수정할 수 없습니다"
            );
        }

        JsonNode oldBody = body == null ? null : parseJson(engineeringChange.getBody());
        if (title != null) {
            engineeringChange.updateTitle(title, actorId);
        }
        if (body != null) {
            tipTapValidator.validateDocument(body);
            engineeringChange.updateBody(toBodyString(body), actorId);
            registerMentions(
                    engineeringChange.getId(),
                    actorId,
                    body,
                    oldBody,
                    false,
                    engineeringChange.getNumber(),
                    engineeringChange.getTitle(),
                    "engineering_change"
            );
        }
        return engineeringChange;
    }

    public EngineeringChange submitEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        String oldState = engineeringChange.getEngineeringChangeState().name();
        try {
            engineeringChange.submit(actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getEngineeringChangeState().name()
        );
        return engineeringChange;
    }

    public EngineeringChange mergeEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        String oldState = engineeringChange.getEngineeringChangeState().name();
        try {
            engineeringChange.merge(Instant.now(), actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getEngineeringChangeState().name()
        );
        return engineeringChange;
    }

    public EngineeringChange closeEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        String oldState = engineeringChange.getEngineeringChangeState().name();
        try {
            engineeringChange.close(Instant.now(), actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getEngineeringChangeState().name()
        );
        return engineeringChange;
    }

    public EngineeringChange reopenEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        String oldState = engineeringChange.getEngineeringChangeState().name();
        try {
            engineeringChange.reopen(actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getEngineeringChangeState().name()
        );
        return engineeringChange;
    }

    public DiffResult syncReviewers(UUID actorId, UUID engineeringChangeId, List<UUID> userIds, boolean emitActivity) {
        Set<UUID> current = engineeringChangeReviewerRepository.findByEngineeringChangeId(engineeringChangeId).stream()
                .map(EngineeringChangeReviewer::getUserId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(userIds);

        Set<UUID> reviewerTeamIds = engineeringChangeTeamReviewerRepository.findByEngineeringChangeId(engineeringChangeId).stream()
                .map(EngineeringChangeTeamReviewer::getTeamId)
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
            engineeringChangeReviewerRepository.deleteByEngineeringChangeIdAndUserIdIn(engineeringChangeId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            EngineeringChange engineeringChange = getEngineeringChangeOrThrow(engineeringChangeId);
            engineeringChangeReviewerRepository.saveAll(toAdd.stream()
                    .map(engineeringChange::assignReviewer)
                    .toList());
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Map<UUID, User> users = findUsers(Set.copyOf(union(toAdd, toRemove)));
            addDiffActivity(
                    engineeringChangeId,
                    actorId,
                    ACTION_REVIEWER_CHANGED,
                    toAdd.stream().map(userId -> toUserRef(userId, users.get(userId))).toList(),
                    toRemove.stream().map(userId -> toUserRef(userId, users.get(userId))).toList()
            );
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncTeamReviewers(UUID engineeringChangeId, List<UUID> teamIds) {
        Set<UUID> current = engineeringChangeTeamReviewerRepository.findByEngineeringChangeId(engineeringChangeId).stream()
                .map(EngineeringChangeTeamReviewer::getTeamId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(teamIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            engineeringChangeTeamReviewerRepository.deleteByEngineeringChangeIdAndTeamIdIn(engineeringChangeId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            EngineeringChange engineeringChange = getEngineeringChangeOrThrow(engineeringChangeId);
            engineeringChangeTeamReviewerRepository.saveAll(toAdd.stream()
                    .map(engineeringChange::assignTeamReviewer)
                    .toList());

            Set<UUID> overlapUsers = teamMemberRepository.findByTeam_IdIn(toAdd).stream()
                    .map(TeamMember::getUserId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!overlapUsers.isEmpty()) {
                engineeringChangeReviewerRepository.deleteByEngineeringChangeIdAndUserIdIn(
                        engineeringChangeId,
                        overlapUsers
                );
            }
        }

        return new DiffResult(toAdd, toRemove);
    }

    public DiffResult syncIssues(UUID actorId, UUID engineeringChangeId, List<UUID> issueIds, boolean emitActivity) {
        validateIssueIds(issueIds);

        Set<UUID> current = engineeringChangeIssueRepository.findByEngineeringChangeId(engineeringChangeId).stream()
                .map(EngineeringChangeIssueLink::getIssueId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(issueIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            engineeringChangeIssueRepository.deleteByEngineeringChangeIdAndIssueIdIn(engineeringChangeId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            EngineeringChange engineeringChange = getEngineeringChangeOrThrow(engineeringChangeId);
            engineeringChangeIssueRepository.saveAll(toAdd.stream()
                    .map(engineeringChange::linkIssue)
                    .toList());
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            EngineeringChange engineeringChange = getEngineeringChangeOrThrow(engineeringChangeId);
            Map<UUID, Issue> issues = findIssues(Set.copyOf(union(toAdd, toRemove)));

            addDiffActivity(
                    engineeringChangeId,
                    actorId,
                    ACTION_ENGINEERING_CHANGE_ISSUE_CHANGED,
                    toAdd.stream().map(issueId -> toIssueRef(issues.get(issueId))).toList(),
                    toRemove.stream().map(issueId -> toIssueRef(issues.get(issueId))).toList()
            );

            Map<String, Object> engineeringChangeRef = toEngineeringChangeRef(engineeringChange);
            for (UUID addedIssueId : toAdd) {
                addDiffActivity(
                        addedIssueId,
                        actorId,
                        ACTION_ISSUE_ENGINEERING_CHANGE_CHANGED,
                        List.of(engineeringChangeRef),
                        List.of()
                );
            }
            for (UUID removedIssueId : toRemove) {
                addDiffActivity(
                        removedIssueId,
                        actorId,
                        ACTION_ISSUE_ENGINEERING_CHANGE_CHANGED,
                        List.of(),
                        List.of(engineeringChangeRef)
                );
            }
        }

        return new DiffResult(toAdd, toRemove);
    }

    public void recordEngineeringChangePartRevisionDiffActivity(
            UUID actorId,
            UUID engineeringChangeId,
            List<EngineeringChangePartRevisionSnapshot> added,
            List<EngineeringChangePartRevisionSnapshot> removed
    ) {
        if ((added == null || added.isEmpty()) && (removed == null || removed.isEmpty())) {
            return;
        }
        addDiffActivity(
                engineeringChangeId,
                actorId,
                ACTION_ENGINEERING_CHANGE_PART_REVISION_CHANGED,
                added == null ? List.of() : added.stream().map(this::toPartRevisionRef).toList(),
                removed == null ? List.of() : removed.stream().map(this::toPartRevisionRef).toList()
        );
    }

    public EngineeringChangeReviewer submitReview(UUID actorId, UUID engineeringChangeId, ReviewStatus status) {
        if (status == null || status == ReviewStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "리뷰 상태는 APPROVED 또는 REJECTED만 허용됩니다");
        }
        EngineeringChangeReviewer reviewer = engineeringChangeReviewerRepository
                .findByEngineeringChangeIdAndUserId(engineeringChangeId, actorId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "해당 변경관리의 검토자가 아닙니다"));
        reviewer.submit(status, Instant.now());
        return reviewer;
    }

    public AbstractComment createComment(UUID actorId, UUID engineeringChangeId, JsonNode body) {
        tipTapValidator.validateDocument(body);
        MentionSource source = getMentionSourceOrThrow(engineeringChangeId);
        EngineeringChange engineeringChange = getEngineeringChangeOrThrow(engineeringChangeId);
        EngineeringChangeComment comment = engineeringChange.writeComment(toBodyString(body), actorId);
        engineeringChangeCommentRepository.save(comment);

        registerMentions(
                engineeringChangeId,
                actorId,
                body,
                null,
                true,
                source.number(),
                source.title(),
                source.type()
        );
        return comment;
    }

    public AbstractComment updateComment(UUID actorId, UUID engineeringChangeId, UUID commentId, JsonNode body) {
        tipTapValidator.validateDocument(body);
        MentionSource source = getMentionSourceOrThrow(engineeringChangeId);
        EngineeringChangeComment comment = findCommentOrThrow(engineeringChangeId, commentId);
        if (!comment.getCreatedBy().equals(actorId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "본인이 작성한 댓글만 수정할 수 있습니다");
        }

        JsonNode oldBody = parseJson(comment.getBody());
        comment.updateBody(toBodyString(body), actorId);
        registerMentions(
                engineeringChangeId,
                actorId,
                body,
                oldBody,
                true,
                source.number(),
                source.title(),
                source.type()
        );
        return comment;
    }

    public void deleteComment(UUID actorId, UUID engineeringChangeId, UUID commentId) {
        EngineeringChangeComment comment = findCommentOrThrow(engineeringChangeId, commentId);
        if (!comment.getCreatedBy().equals(actorId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "본인이 작성한 댓글만 삭제할 수 있습니다");
        }
        engineeringChangeCommentRepository.delete(comment);
    }

    public List<File> attachFiles(UUID actorId, UUID engineeringChangeId, List<File> files) {
        return attachFiles(actorId, engineeringChangeId, files, true);
    }

    public List<File> attachFiles(UUID actorId, UUID engineeringChangeId, List<File> files, boolean emitActivity) {
        getEngineeringChangeOrThrow(engineeringChangeId);
        if (files.isEmpty()) {
            return List.of();
        }

        for (File file : files) {
            file.assignOwner(OWNER_TYPE_ENGINEERING_CHANGE, engineeringChangeId);
        }
        long totalBytes = files.stream().mapToLong(File::getFileSize).sum();
        if (totalBytes > 0L) {
            organizationApi.consumeStorageForCurrentTenant(totalBytes);
        }

        if (emitActivity) {
            addDiffActivity(
                    engineeringChangeId,
                    actorId,
                    ACTION_FILE_ATTACHED,
                    files.stream().map(this::toFileRef).toList(),
                    List.of()
            );
        }
        return files;
    }

    public void detachFile(UUID actorId, UUID engineeringChangeId, UUID fileId) {
        getEngineeringChangeOrThrow(engineeringChangeId);

        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                        fileId,
                        OWNER_TYPE_ENGINEERING_CHANGE,
                        engineeringChangeId
                )
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "해당 변경관리에 연결된 파일을 찾을 수 없습니다"));

        String fileName = file.getOriginalName();
        long fileSize = file.getFileSize();
        file.softDelete(actorId);
        if (fileSize > 0L) {
            organizationApi.releaseStorageForCurrentTenant(fileSize);
        }

        Map<String, Object> removed = new LinkedHashMap<>();
        removed.put("id", fileId.toString());
        removed.put("type", "file");
        removed.put("label", fileName == null ? "(알 수 없음)" : fileName);
        addDiffActivity(engineeringChangeId, actorId, ACTION_FILE_DETACHED, List.of(), List.of(removed));
    }

    private int allocateIssueNumber() {
        IssueNumberSequence sequence = issueNumberSequenceRepository.findByIdForUpdate(ISSUE_NUMBER_SEQUENCE_ID)
                .orElseGet(this::initializeIssueNumberSequence);
        return sequence.allocateNextNumber();
    }

    private IssueNumberSequence initializeIssueNumberSequence() {
        int nextIssueNumber = issueRepository.findTopByOrderByNumberDesc()
                .map(issue -> issue.getNumber() + 1)
                .orElse(1);
        int nextEngineeringChangeNumber = engineeringChangeRepository.findAllByOrderByNumberDesc(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(item -> item.getNumber() + 1)
                .orElse(1);
        int nextNumber = Math.max(nextIssueNumber, nextEngineeringChangeNumber);

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

    private void validateIssueIds(Iterable<UUID> issueIds) {
        for (UUID issueId : issueIds) {
            if (issueRepository.findById(issueId).isEmpty()) {
                throw new AppException(ErrorCode.NOT_FOUND, "Issue '" + issueId + "'을(를) 찾을 수 없습니다");
            }
        }
    }

    private void addStateActivity(UUID targetId, UUID actorId, ActivityAction action, String oldState, String newState) {
        addActivity(
                targetId,
                actorId,
                action,
                Map.of("changes", Map.of("state", Map.of("old", oldState, "new", newState)))
        );
    }

    private void addDiffActivity(
            UUID targetId,
            UUID actorId,
            ActivityAction action,
            List<Map<String, Object>> added,
            List<Map<String, Object>> removed
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("added", added);
        detail.put("removed", removed);
        addActivity(targetId, actorId, action, detail);
    }

    private void addActivity(UUID targetId, UUID actorId, ActivityAction action, Object detail) {
        activityRepository.save(Activity.create(
                resolveActivityTargetType(targetId),
                targetId,
                action.value(),
                actorId,
                toJsonString(detail)
        ));
    }

    private ActivityTargetType resolveActivityTargetType(UUID targetId) {
        if (engineeringChangeRepository.existsById(targetId)) {
            return ActivityTargetType.ENGINEERING_CHANGE;
        }
        if (issueRepository.existsById(targetId)) {
            return ActivityTargetType.ISSUE;
        }
        return ActivityTargetType.ENGINEERING_CHANGE;
    }

    private EngineeringChangeComment findCommentOrThrow(UUID engineeringChangeId, UUID commentId) {
        EngineeringChangeComment comment = engineeringChangeCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        if (!comment.getEngineeringChangeId().equals(engineeringChangeId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "해당 변경관리의 댓글이 아닙니다");
        }
        return comment;
    }

    private void registerMentions(
            UUID sourceId,
            UUID actorId,
            JsonNode newBody,
            JsonNode oldBody,
            boolean isComment,
            int sourceNumber,
            String sourceTitle,
            String sourceType
    ) {
        MentionExtractor.MentionSet newMentions = mentionExtractor.extract(newBody);
        MentionExtractor.MentionSet oldMentions = mentionExtractor.extract(oldBody);

        Set<UUID> addedIssueMentions = new LinkedHashSet<>(newMentions.issueIds());
        addedIssueMentions.removeAll(oldMentions.issueIds());
        addedIssueMentions.remove(sourceId);
        for (UUID targetIssueId : addedIssueMentions) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("id", sourceId.toString());
            ref.put("type", sourceType);
            ref.put("label", "#" + sourceNumber + " " + sourceTitle);
            ref.put("meta", Map.of("number", sourceNumber, "is_comment", isComment));
            addActivity(targetIssueId, actorId, ACTION_ISSUE_MENTIONED, Map.of("refs", List.of(ref)));
        }

        Set<UUID> addedUserMentions = new LinkedHashSet<>(newMentions.userIds());
        addedUserMentions.removeAll(oldMentions.userIds());
        addedUserMentions.remove(actorId);
        if (!addedUserMentions.isEmpty()) {
            applicationEventPublisher.publishEvent(IssueUsersMentionedEvent.create(
                    sourceId,
                    actorId,
                    addedUserMentions,
                    sourceNumber,
                    sourceTitle,
                    sourceType,
                    isComment
            ));
        }
    }

    private MentionSource getMentionSourceOrThrow(UUID engineeringChangeId) {
        EngineeringChange engineeringChange = engineeringChangeRepository.findById(engineeringChangeId).orElse(null);
        if (engineeringChange == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "대상을 찾을 수 없습니다");
        }
        return new MentionSource(
                engineeringChange.getId(),
                engineeringChange.getNumber(),
                engineeringChange.getTitle(),
                "engineering_change"
        );
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
        Map<UUID, User> users = new HashMap<>();
        for (User user : userRepository.findByIdInOrderByFullNameAsc(userIds)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private Map<UUID, Issue> findIssues(Set<UUID> issueIds) {
        if (issueIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Issue> issues = new HashMap<>();
        for (Issue issue : issueRepository.findAllById(issueIds)) {
            issues.put(issue.getId(), issue);
        }
        return issues;
    }

    private Set<UUID> union(Set<UUID> a, Set<UUID> b) {
        Set<UUID> result = new LinkedHashSet<>(a);
        result.addAll(b);
        return result;
    }

    private Map<String, Object> toUserRef(UUID userId, User user) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", userId.toString());
        ref.put("type", "user");
        ref.put("label", user == null ? "(알 수 없음)" : user.getFullName());
        return ref;
    }

    private Map<String, Object> toPartRevisionRef(EngineeringChangePartRevisionSnapshot snapshot) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", snapshot.revisionId().toString());
        ref.put("type", "part_revision");
        ref.put(
                "label",
                snapshot.baseRevisionCode() == null
                        ? "%s/%s".formatted(snapshot.partNumber(), snapshot.draftKey())
                        : "%s/%s/%s".formatted(snapshot.partNumber(), snapshot.baseRevisionCode(), snapshot.draftKey())
        );
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("partNumber", snapshot.partNumber());
        meta.put("baseRevisionCode", snapshot.baseRevisionCode());
        meta.put("draftKey", snapshot.draftKey());
        meta.put("status", snapshot.status().name());
        ref.put("meta", meta);
        return ref;
    }

    private Map<String, Object> toIssueRef(Issue issue) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", issue == null ? "" : issue.getId().toString());
        ref.put("type", "issue");
        ref.put("label", issue == null ? "(알 수 없음)" : "#" + issue.getNumber() + " " + issue.getTitle());
        ref.put("meta", Map.of("number", issue == null ? 0 : issue.getNumber()));
        return ref;
    }

    private Map<String, Object> toEngineeringChangeRef(EngineeringChange engineeringChange) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", engineeringChange == null ? "" : engineeringChange.getId().toString());
        ref.put("type", "engineering_change");
        ref.put(
                "label",
                engineeringChange == null ? "(알 수 없음)" : "#" + engineeringChange.getNumber() + " " + engineeringChange.getTitle()
        );
        ref.put("meta", Map.of("number", engineeringChange == null ? 0 : engineeringChange.getNumber()));
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

    private record MentionSource(UUID id, int number, String title, String type) {
    }
}
