package com.fabbitinc.server.application.engineeringchange.service;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.issue.api.IssueSnapshot;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.api.EngineeringChangePartRevisionSnapshot;
import com.fabbitinc.server.application.workitem.event.WorkItemUsersMentionedEvent;
import com.fabbitinc.server.application.workitem.support.MentionExtractor;
import com.fabbitinc.server.application.workitem.support.TipTapValidator;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeComment;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
import com.fabbitinc.server.domain.workitem.model.WorkItemNumberSequence;
import com.fabbitinc.server.domain.workitem.repository.WorkItemNumberSequenceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EngineeringChangeService {

    private static final String OWNER_TYPE_ENGINEERING_CHANGE = "engineering_change";
    private static final UUID WORK_ITEM_NUMBER_SEQUENCE_ID = UUID.fromString("89d98a7b-6b53-4e63-a02a-73f66f606703");

    private static final ActivityAction ACTION_ISSUE_STATE_CHANGED = ActivityAction.ISSUE_STATE_CHANGED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_STATE_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_STATE_CHANGED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_STEP_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_STEP_CHANGED;
    private static final ActivityAction ACTION_FILE_ATTACHED = ActivityAction.ENGINEERING_CHANGE_FILE_ATTACHED;
    private static final ActivityAction ACTION_FILE_DETACHED = ActivityAction.ENGINEERING_CHANGE_FILE_DETACHED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_ISSUE_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_ISSUE_CHANGED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_PART_REVISION_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_PART_REVISION_CHANGED;
    private static final ActivityAction ACTION_ISSUE_ENGINEERING_CHANGE_CHANGED =
            ActivityAction.ISSUE_ENGINEERING_CHANGE_CHANGED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_MENTIONED =
            ActivityAction.ENGINEERING_CHANGE_MENTIONED;

    private final IssueApi issueApi;
    private final WorkItemNumberSequenceRepository workItemNumberSequenceRepository;
    private final EngineeringChangeRepository engineeringChangeRepository;
    private final EngineeringChangeStepRepository engineeringChangeStepRepository;
    private final EngineeringChangeIssueLinkRepository engineeringChangeIssueRepository;
    private final EngineeringChangeCommentRepository engineeringChangeCommentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrganizationApi organizationApi;
    private final TipTapValidator tipTapValidator;
    private final MentionExtractor mentionExtractor;
    private final ObjectMapper objectMapper;

    public EngineeringChange getEngineeringChangeByNumberOrThrow(int engineeringChangeNumber) {
        return engineeringChangeRepository.findByNumber(engineeringChangeNumber)
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
                allocateWorkItemNumber(),
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
        assertDraftEditable(engineeringChange);

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

    public void replaceSteps(
            UUID actorId,
            EngineeringChange engineeringChange,
            List<StepDraft> requestedSteps,
            boolean emitActivity
    ) {
        assertDraftEditable(engineeringChange);

        List<StepDraft> normalizedSteps = normalizeSteps(requestedSteps);
        validateStepDrafts(normalizedSteps);

        List<EngineeringChangeStep> currentSteps =
                engineeringChangeStepRepository.findByEngineeringChangeIdOrderBySequenceAscCreatedAtAsc(engineeringChange.getId());
        Map<UUID, User> users = findUsers(collectUserIds(currentSteps, normalizedSteps));
        Map<UUID, String> teamNames = findTeamNames(collectTeamIds(currentSteps, normalizedSteps));

        List<Map<String, Object>> removedRefs = currentSteps.stream()
                .sorted(stepComparator())
                .map(step -> toStepRef(step, users, teamNames))
                .toList();

        engineeringChange.clearSteps(actorId);
        for (StepDraft step : normalizedSteps) {
            engineeringChange.addStep(
                    step.stepType(),
                    step.assigneeType(),
                    step.assigneeId(),
                    step.sequence(),
                    actorId
            );
        }

        if (emitActivity && (!removedRefs.isEmpty() || !normalizedSteps.isEmpty())) {
            List<Map<String, Object>> addedRefs = engineeringChange.getSteps().stream()
                    .sorted(stepComparator())
                    .map(step -> toStepRef(step, users, teamNames))
                    .toList();
            addDiffActivity(
                    engineeringChange.getId(),
                    actorId,
                    ACTION_ENGINEERING_CHANGE_STEP_CHANGED,
                    addedRefs,
                    removedRefs
            );
        }
    }

    public EngineeringChange submitEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        validateReadyForSubmit(engineeringChange);
        resetSteps(engineeringChange);
        String oldState = engineeringChange.getState().name();
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
                engineeringChange.getState().name()
        );
        return engineeringChange;
    }

    public EngineeringChangeStep approveReviewStep(UUID actorId, EngineeringChange engineeringChange) {
        if (engineeringChange.getState() != EngineeringChangeState.REVIEW_PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE, "REVIEW_PENDING 상태에서만 검토를 승인할 수 있습니다");
        }

        EngineeringChangeStep step = findActionablePendingStep(actorId, engineeringChange, EngineeringChangeStepType.REVIEW);
        step.approve(actorId, Instant.now());
        transitionToApprovalPendingIfReviewCompleted(actorId, engineeringChange);
        return step;
    }

    public EngineeringChange approveEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        if (engineeringChange.getState() != EngineeringChangeState.APPROVAL_PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE, "APPROVAL_PENDING 상태에서만 승인할 수 있습니다");
        }

        EngineeringChangeStep step = findActionablePendingStep(actorId, engineeringChange, EngineeringChangeStepType.APPROVAL);
        step.approve(actorId, Instant.now());
        if (hasPendingStageStep(engineeringChange.getId(), EngineeringChangeStepType.APPROVAL)) {
            return engineeringChange;
        }

        String oldState = engineeringChange.getState().name();
        try {
            engineeringChange.approve(actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getState().name()
        );
        return engineeringChange;
    }

    public boolean approveReleaseStep(UUID actorId, EngineeringChange engineeringChange) {
        if (engineeringChange.getState() != EngineeringChangeState.RELEASE_PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE, "RELEASE_PENDING 상태에서만 반영 단계를 진행할 수 있습니다");
        }

        EngineeringChangeStep step = findActionablePendingStep(actorId, engineeringChange, EngineeringChangeStepType.RELEASE);
        step.approve(actorId, Instant.now());
        return !hasPendingStageStep(engineeringChange.getId(), EngineeringChangeStepType.RELEASE);
    }

    public EngineeringChange completeRelease(UUID actorId, EngineeringChange engineeringChange) {
        if (engineeringChange.getState() != EngineeringChangeState.RELEASE_PENDING) {
            throw new AppException(ErrorCode.INVALID_STATE, "RELEASE_PENDING 상태에서만 반영 완료할 수 있습니다");
        }
        if (hasPendingStageStep(engineeringChange.getId(), EngineeringChangeStepType.RELEASE)) {
            throw new AppException(ErrorCode.CONFLICT, "남아 있는 반영 단계가 있어 완료할 수 없습니다");
        }

        String oldState = engineeringChange.getState().name();
        try {
            engineeringChange.release(Instant.now(), actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getState().name()
        );
        return engineeringChange;
    }

    public EngineeringChange rejectEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        EngineeringChangeStepType currentStepType = resolveCurrentStepType(engineeringChange.getState());
        EngineeringChangeStep step = findActionablePendingStep(actorId, engineeringChange, currentStepType);
        step.reject(actorId, Instant.now());

        String oldState = engineeringChange.getState().name();
        try {
            engineeringChange.reject(actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getState().name()
        );
        return engineeringChange;
    }

    public EngineeringChange cancelEngineeringChange(UUID actorId, EngineeringChange engineeringChange) {
        String oldState = engineeringChange.getState().name();
        try {
            engineeringChange.cancel(Instant.now(), actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getState().name()
        );
        return engineeringChange;
    }

    public DiffResult syncIssues(UUID actorId, UUID engineeringChangeId, List<UUID> issueIds, boolean emitActivity) {
        assertDraftEditable(getEngineeringChangeOrThrow(engineeringChangeId));
        issueApi.validateIssueIds(issueIds);

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
            Map<UUID, IssueSnapshot> issues = issueApi.getIssueSnapshotMap(Set.copyOf(union(toAdd, toRemove)));

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
        assertDraftEditable(getEngineeringChangeOrThrow(engineeringChangeId));
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

    private int allocateWorkItemNumber() {
        WorkItemNumberSequence sequence = workItemNumberSequenceRepository.findByIdForUpdate(WORK_ITEM_NUMBER_SEQUENCE_ID)
                .orElseGet(this::initializeWorkItemNumberSequence);
        return sequence.allocateNextNumber();
    }

    private WorkItemNumberSequence initializeWorkItemNumberSequence() {
        int nextIssueNumber = issueApi.getNextIssueNumberSeed();
        int nextEngineeringChangeNumber = engineeringChangeRepository.findAllByOrderByNumberDesc(org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(item -> item.getNumber() + 1)
                .orElse(1);
        int nextNumber = Math.max(nextIssueNumber, nextEngineeringChangeNumber);

        try {
            return workItemNumberSequenceRepository.saveAndFlush(
                    WorkItemNumberSequence.initialize(WORK_ITEM_NUMBER_SEQUENCE_ID, nextNumber)
            );
        } catch (DataIntegrityViolationException ex) {
            return workItemNumberSequenceRepository.findByIdForUpdate(WORK_ITEM_NUMBER_SEQUENCE_ID)
                    .orElseThrow(() -> new AppException(
                            ErrorCode.INTERNAL_SERVER_ERROR,
                            "워크아이템 번호 시퀀스를 초기화할 수 없습니다"
                    ));
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
        if (issueApi.existsIssue(targetId)) {
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
            addActivity(targetIssueId, actorId, ACTION_ENGINEERING_CHANGE_MENTIONED, Map.of("refs", List.of(ref)));
        }

        Set<UUID> addedUserMentions = new LinkedHashSet<>(newMentions.userIds());
        addedUserMentions.removeAll(oldMentions.userIds());
        addedUserMentions.remove(actorId);
        if (!addedUserMentions.isEmpty()) {
            applicationEventPublisher.publishEvent(WorkItemUsersMentionedEvent.create(
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

    private void assertDraftEditable(EngineeringChange engineeringChange) {
        if (engineeringChange.getState() != EngineeringChangeState.DRAFT) {
            throw new AppException(ErrorCode.INVALID_STATE, "DRAFT 상태의 변경안만 수정할 수 있습니다");
        }
    }

    private void validateReadyForSubmit(EngineeringChange engineeringChange) {
        if (engineeringChange.getState() != EngineeringChangeState.DRAFT) {
            throw new AppException(ErrorCode.INVALID_STATE, "DRAFT 상태에서만 제출할 수 있습니다");
        }

        List<EngineeringChangeStep> steps =
                engineeringChangeStepRepository.findByEngineeringChangeIdOrderBySequenceAscCreatedAtAsc(engineeringChange.getId());
        if (steps.isEmpty()) {
            throw new AppException(ErrorCode.CONFLICT, "단계를 먼저 지정해야 합니다");
        }

        long reviewCount = steps.stream()
                .filter(step -> step.getStepType() == EngineeringChangeStepType.REVIEW)
                .count();
        long approvalCount = steps.stream()
                .filter(step -> step.getStepType() == EngineeringChangeStepType.APPROVAL)
                .count();
        long releaseCount = steps.stream()
                .filter(step -> step.getStepType() == EngineeringChangeStepType.RELEASE)
                .count();

        if (reviewCount < 1) {
            throw new AppException(ErrorCode.CONFLICT, "검토 단계를 최소 1개 이상 지정해야 합니다");
        }
        if (approvalCount < 1) {
            throw new AppException(ErrorCode.CONFLICT, "승인 단계를 최소 1개 이상 지정해야 합니다");
        }
        if (releaseCount < 1) {
            throw new AppException(ErrorCode.CONFLICT, "반영 단계를 최소 1개 이상 지정해야 합니다");
        }
    }

    private void resetSteps(EngineeringChange engineeringChange) {
        engineeringChangeStepRepository.findByEngineeringChangeIdOrderBySequenceAscCreatedAtAsc(engineeringChange.getId())
                .forEach(EngineeringChangeStep::reset);
    }

    private void transitionToApprovalPendingIfReviewCompleted(UUID actorId, EngineeringChange engineeringChange) {
        if (engineeringChange.getState() != EngineeringChangeState.REVIEW_PENDING) {
            return;
        }
        if (hasPendingStageStep(engineeringChange.getId(), EngineeringChangeStepType.REVIEW)) {
            return;
        }
        String oldState = engineeringChange.getState().name();
        engineeringChange.completeReview(actorId);
        addStateActivity(
                engineeringChange.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                engineeringChange.getState().name()
        );
    }

    private EngineeringChangeStep findActionablePendingStep(
            UUID actorId,
            EngineeringChange engineeringChange,
            EngineeringChangeStepType stepType
    ) {
        List<EngineeringChangeStep> pendingSteps = engineeringChangeStepRepository
                .findByEngineeringChangeIdAndStepTypeAndStatusOrderBySequenceAscCreatedAtAsc(
                        engineeringChange.getId(),
                        stepType,
                        EngineeringChangeStepStatus.PENDING
                );
        if (pendingSteps.isEmpty()) {
            throw new AppException(ErrorCode.CONFLICT, "진행 가능한 단계가 없습니다");
        }

        int currentSequence = pendingSteps.stream()
                .mapToInt(EngineeringChangeStep::getSequence)
                .min()
                .orElseThrow(() -> new AppException(ErrorCode.CONFLICT, "진행 가능한 단계가 없습니다"));

        List<EngineeringChangeStep> activeSteps = pendingSteps.stream()
                .filter(step -> step.getSequence() == currentSequence)
                .toList();

        Set<UUID> activeTeamIds = activeSteps.stream()
                .filter(step -> step.getAssigneeType() == EngineeringChangeStepAssigneeType.TEAM)
                .map(EngineeringChangeStep::getAssigneeId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> actorTeamIds = teamMemberRepository.findByTeam_IdIn(activeTeamIds).stream()
                .filter(member -> actorId.equals(member.getUserId()))
                .map(TeamMember::getTeamId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return activeSteps.stream()
                .filter(step -> step.isAssignedToUser(actorId)
                        || (step.getAssigneeType() == EngineeringChangeStepAssigneeType.TEAM
                        && actorTeamIds.contains(step.getAssigneeId())))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, forbiddenMessage(stepType)));
    }

    private boolean hasPendingStageStep(UUID engineeringChangeId, EngineeringChangeStepType stepType) {
        return !engineeringChangeStepRepository
                .findByEngineeringChangeIdAndStepTypeAndStatusOrderBySequenceAscCreatedAtAsc(
                        engineeringChangeId,
                        stepType,
                        EngineeringChangeStepStatus.PENDING
                )
                .isEmpty();
    }

    private EngineeringChangeStepType resolveCurrentStepType(EngineeringChangeState state) {
        return switch (state) {
            case REVIEW_PENDING -> EngineeringChangeStepType.REVIEW;
            case APPROVAL_PENDING -> EngineeringChangeStepType.APPROVAL;
            case RELEASE_PENDING -> EngineeringChangeStepType.RELEASE;
            default -> throw new AppException(ErrorCode.INVALID_STATE, "대기 상태에서만 처리할 수 있습니다");
        };
    }

    private String forbiddenMessage(EngineeringChangeStepType stepType) {
        return switch (stepType) {
            case REVIEW -> "현재 검토 단계 담당자만 처리할 수 있습니다";
            case APPROVAL -> "현재 승인 단계 담당자만 처리할 수 있습니다";
            case RELEASE -> "현재 반영 단계 담당자만 처리할 수 있습니다";
        };
    }

    private List<StepDraft> normalizeSteps(List<StepDraft> requestedSteps) {
        if (requestedSteps == null) {
            return List.of();
        }
        return requestedSteps.stream()
                .sorted(Comparator
                        .comparing(StepDraft::stepType)
                        .thenComparingInt(StepDraft::sequence)
                        .thenComparing(StepDraft::assigneeType)
                        .thenComparing(StepDraft::assigneeId))
                .toList();
    }

    private void validateStepDrafts(List<StepDraft> requestedSteps) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (StepDraft step : requestedSteps) {
            validateAssignee(step);
            String uniqueKey = step.stepType() + ":" + step.assigneeType() + ":" + step.assigneeId() + ":" + step.sequence();
            if (!uniqueKeys.add(uniqueKey)) {
                throw new AppException(ErrorCode.CONFLICT, "중복된 단계가 포함되어 있습니다");
            }
        }
    }

    private void validateAssignee(StepDraft step) {
        if (step.assigneeType() == EngineeringChangeStepAssigneeType.USER) {
            userRepository.findById(step.assigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "단계 담당 사용자를 찾을 수 없습니다"));
            return;
        }
        if (!teamRepository.existsById(step.assigneeId())) {
            throw new AppException(ErrorCode.NOT_FOUND, "단계 담당 팀을 찾을 수 없습니다");
        }
    }

    private Set<UUID> collectUserIds(List<EngineeringChangeStep> currentSteps, List<StepDraft> requestedSteps) {
        Set<UUID> result = new LinkedHashSet<>();
        for (EngineeringChangeStep step : currentSteps) {
            if (step.getAssigneeType() == EngineeringChangeStepAssigneeType.USER) {
                result.add(step.getAssigneeId());
            }
            if (step.getActedBy() != null) {
                result.add(step.getActedBy());
            }
        }
        for (StepDraft step : requestedSteps) {
            if (step.assigneeType() == EngineeringChangeStepAssigneeType.USER) {
                result.add(step.assigneeId());
            }
        }
        return result;
    }

    private Set<UUID> collectTeamIds(List<EngineeringChangeStep> currentSteps, List<StepDraft> requestedSteps) {
        Set<UUID> result = new LinkedHashSet<>();
        for (EngineeringChangeStep step : currentSteps) {
            if (step.getAssigneeType() == EngineeringChangeStepAssigneeType.TEAM) {
                result.add(step.getAssigneeId());
            }
        }
        for (StepDraft step : requestedSteps) {
            if (step.assigneeType() == EngineeringChangeStepAssigneeType.TEAM) {
                result.add(step.assigneeId());
            }
        }
        return result;
    }

    private Map<UUID, String> findTeamNames(Set<UUID> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> teamNames = new HashMap<>();
        teamRepository.findAllById(teamIds).forEach(team -> teamNames.put(team.getId(), team.getName()));
        return teamNames;
    }

    private Comparator<EngineeringChangeStep> stepComparator() {
        return Comparator
                .comparing(EngineeringChangeStep::getStepType)
                .thenComparingInt(EngineeringChangeStep::getSequence)
                .thenComparing(EngineeringChangeStep::getAssigneeType)
                .thenComparing(EngineeringChangeStep::getAssigneeId);
    }

    private Map<String, Object> toStepRef(
            EngineeringChangeStep step,
            Map<UUID, User> users,
            Map<UUID, String> teamNames
    ) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", step.getId().toString());
        ref.put("type", "engineering_change_step");
        ref.put("step_type", step.getStepType().name());
        ref.put("assignee_type", step.getAssigneeType().name());
        ref.put("sequence", step.getSequence());
        ref.put("status", step.getStatus().name());
        if (step.getAssigneeType() == EngineeringChangeStepAssigneeType.USER) {
            User assignee = users.get(step.getAssigneeId());
            ref.put("label", assignee == null ? step.getAssigneeId().toString() : assignee.getFullName());
            ref.put("assignee_id", step.getAssigneeId().toString());
        } else {
            ref.put("label", teamNames.getOrDefault(step.getAssigneeId(), step.getAssigneeId().toString()));
            ref.put("assignee_id", step.getAssigneeId().toString());
        }
        return ref;
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

    private Map<String, Object> toIssueRef(IssueSnapshot issue) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", issue.id().toString());
        ref.put("type", "issue");
        ref.put("label", "#" + issue.number() + " " + issue.title());
        ref.put("meta", Map.of("number", issue.number(), "state", issue.state()));
        return ref;
    }

    private Map<String, Object> toEngineeringChangeRef(EngineeringChange engineeringChange) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", engineeringChange.getId().toString());
        ref.put("type", "engineering_change");
        ref.put("label", "#" + engineeringChange.getNumber() + " " + engineeringChange.getTitle());
        ref.put("meta", Map.of("number", engineeringChange.getNumber(), "state", engineeringChange.getState()));
        return ref;
    }

    private Map<String, Object> toPartRevisionRef(EngineeringChangePartRevisionSnapshot snapshot) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", snapshot.revisionId().toString());
        ref.put("type", "part_revision");
        ref.put("label", snapshot.partNumber() + " / draft " + snapshot.draftKey());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("part_number", snapshot.partNumber());
        meta.put("base_revision_code", snapshot.baseRevisionCode());
        meta.put("draft_key", snapshot.draftKey());
        meta.put("status", snapshot.status());
        ref.put("meta", meta);
        return ref;
    }

    private Map<String, Object> toFileRef(File file) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", file.getId().toString());
        ref.put("type", "file");
        ref.put("label", file.getOriginalName());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("content_type", file.getContentType());
        meta.put("file_size", file.getFileSize());
        ref.put("meta", meta);
        return ref;
    }

    private record MentionSource(
            UUID id,
            int number,
            String title,
            String type
    ) {
    }

    public record DiffResult(
            Set<UUID> added,
            Set<UUID> removed
    ) {
    }

    public record StepDraft(
            EngineeringChangeStepType stepType,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId,
            int sequence
    ) {
    }
}
