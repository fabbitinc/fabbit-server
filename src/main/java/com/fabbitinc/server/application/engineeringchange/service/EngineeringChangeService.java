package com.fabbitinc.server.application.engineeringchange.service;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.issue.api.IssueSnapshot;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.workitem.event.WorkItemUsersMentionedEvent;
import com.fabbitinc.server.application.workitem.support.MentionExtractor;
import com.fabbitinc.server.application.workitem.support.TipTapValidator;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeComment;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeLabel;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStage;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeLabelRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.StepStageRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
import com.fabbitinc.server.domain.workitem.model.WorkItemNumberSequence;
import com.fabbitinc.server.domain.workitem.repository.WorkItemNumberSequenceRepository;
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
    private static final ActivityAction ACTION_LABEL_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_LABEL_CHANGED;
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
    private final EngineeringChangeLabelRepository engineeringChangeLabelRepository;
    private final EngineeringChangeCommentRepository engineeringChangeCommentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrganizationApi organizationApi;
    private final TipTapValidator tipTapValidator;
    private final MentionExtractor mentionExtractor;
    private final ObjectMapper objectMapper;
    private final StepCompletionEvaluator stepCompletionEvaluator;
    private final StepStageRepository stepStageRepository;

    public EngineeringChange getEngineeringChangeByIdOrThrow(UUID engineeringChangeId) {
        return engineeringChangeRepository.findById(engineeringChangeId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "EngineeringChange '" + engineeringChangeId + "'을(를) 찾을 수 없습니다"
                ));
    }

    public EngineeringChange createEngineeringChange(UUID actorId, String title, JsonNode body, UUID sourceIssueId) {
        tipTapValidator.validateDocument(body);
        EngineeringChange engineeringChange = EngineeringChange.create(
                allocateWorkItemNumber(),
                title,
                toBodyString(body),
                sourceIssueId,
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
        assertMetadataEditable(engineeringChange);

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

    // ── Stage/Step 관리 ──

    public void syncStages(UUID actorId, EngineeringChange ec, List<StageDraft> stageDrafts) {
        assertMetadataEditable(ec);
        validateStageDrafts(stageDrafts);

        Map<UUID, StepStage> existingStagesById = ec.getStages().stream()
                .collect(java.util.stream.Collectors.toMap(StepStage::getId, stage -> stage, (left, right) -> left, LinkedHashMap::new));
        Map<StageKey, StepStage> existingStagesByKey = ec.getStages().stream()
                .collect(java.util.stream.Collectors.toMap(
                        stage -> new StageKey(stage.getStepType(), stage.getSequence()),
                        stage -> stage,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<UUID> retainedStageIds = new LinkedHashSet<>();
        for (StageDraft draft : stageDrafts) {
            StepStage stage = resolveStageForSync(draft, existingStagesById, existingStagesByKey, retainedStageIds);
            if (stage == null) {
                stage = ec.addStage(
                        draft.stepType(),
                        draft.sequence(),
                        draft.completionPolicy(),
                        draft.minApprovals(),
                        draft.deadline(),
                        actorId
                );
            } else {
                stage.reconfigure(
                        draft.stepType(),
                        draft.sequence(),
                        draft.completionPolicy(),
                        draft.minApprovals(),
                        draft.deadline()
                );
            }
            retainedStageIds.add(stage.getId());
            syncStageAssignees(actorId, ec, stage, draft.assignees());
        }

        removeObsoleteStages(actorId, ec, retainedStageIds);
    }

    public EngineeringChange submitEngineeringChange(UUID actorId, EngineeringChange ec) {
        String oldState = ec.getState().name();
        try {
            ec.submit(actorId);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
        addStateActivity(
                ec.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                ec.getState().name()
        );
        return ec;
    }

    public EngineeringChangeStep approveStep(UUID actorId, EngineeringChange ec, UUID stepId) {
        EngineeringChangeStep step = findActionableStep(actorId, ec, stepId);
        String oldState = ec.getState().name();

        step.approve(actorId, Instant.now());

        // Stage 완료 평가
        StepStage stage = stepStageRepository.findById(step.getStepStageId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "단계(Stage)를 찾을 수 없습니다"));
        List<EngineeringChangeStep> stageSteps = ec.getSteps().stream()
                .filter(s -> s.getStepStageId().equals(stage.getId()))
                .toList();
        StageEvaluationResult result = stepCompletionEvaluator.evaluate(stage, stageSteps);

        if (result.complete()) {
            for (UUID cancelId : result.stepsToCancelIds()) {
                ec.getSteps().stream()
                        .filter(s -> s.getId().equals(cancelId))
                        .findFirst()
                        .ifPresent(EngineeringChangeStep::cancel);
            }
            ec.syncStateFromStages(actorId);
        }

        addStepActivity(ec.getId(), actorId, ActivityAction.ENGINEERING_CHANGE_STEP_APPROVED, step, null);

        if (!oldState.equals(ec.getState().name())) {
            addStateActivity(
                    ec.getId(),
                    actorId,
                    ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                    oldState,
                    ec.getState().name()
            );
        }

        return step;
    }

    public EngineeringChange rejectStep(UUID actorId, EngineeringChange ec, UUID stepId, String comment) {
        EngineeringChangeStep step = findActionableStep(actorId, ec, stepId);
        String oldState = ec.getState().name();

        step.reject(actorId, Instant.now());
        ec.resetAllSteps(actorId);

        addStepActivity(ec.getId(), actorId, ActivityAction.ENGINEERING_CHANGE_STEP_REJECTED, step, comment);
        addStateActivity(
                ec.getId(),
                actorId,
                ACTION_ENGINEERING_CHANGE_STATE_CHANGED,
                oldState,
                ec.getState().name()
        );

        return ec;
    }

    public EngineeringChangeStep requestChangesOnStep(UUID actorId, EngineeringChange ec, UUID stepId, String comment) {
        EngineeringChangeStep step = findActionableStep(actorId, ec, stepId);

        step.requestChanges(actorId, Instant.now());

        addStepActivity(ec.getId(), actorId, ActivityAction.ENGINEERING_CHANGE_STEP_CHANGES_REQUESTED, step, comment);

        return step;
    }

    public EngineeringChangeStep resubmitStep(UUID actorId, EngineeringChange ec, UUID stepId) {
        EngineeringChangeStep step = ec.getSteps().stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "단계를 찾을 수 없습니다"));

        if (!step.isChangesRequested()) {
            throw new AppException(ErrorCode.INVALID_STATE, "수정 요청 상태의 단계만 재제출할 수 있습니다");
        }
        if (!actorId.equals(ec.getCreatedBy())) {
            throw new AppException(ErrorCode.FORBIDDEN, "변경안 작성자만 재제출할 수 있습니다");
        }

        step.resubmit();

        addStepActivity(ec.getId(), actorId, ActivityAction.ENGINEERING_CHANGE_STEP_RESUBMITTED, step, null);

        return step;
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

    // ── Issue 동기화 ──

    public DiffResult syncIssues(UUID actorId, UUID engineeringChangeId, List<UUID> issueIds, boolean emitActivity) {
        assertMetadataEditable(getEngineeringChangeByIdOrThrow(engineeringChangeId));
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
            EngineeringChange engineeringChange = getEngineeringChangeByIdOrThrow(engineeringChangeId);
            engineeringChangeIssueRepository.saveAll(toAdd.stream()
                    .map(engineeringChange::linkIssue)
                    .toList());
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            EngineeringChange engineeringChange = getEngineeringChangeByIdOrThrow(engineeringChangeId);
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

    public DiffResult syncLabels(UUID actorId, UUID engineeringChangeId, List<UUID> labelIds, boolean emitActivity) {
        assertMetadataEditable(getEngineeringChangeByIdOrThrow(engineeringChangeId));
        validateLabels(labelIds);

        Set<UUID> current = engineeringChangeLabelRepository.findByEngineeringChangeId(engineeringChangeId).stream()
                .map(EngineeringChangeLabel::getLabelId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(labelIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            engineeringChangeLabelRepository.deleteByEngineeringChangeIdAndLabelIdIn(engineeringChangeId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            EngineeringChange engineeringChange = getEngineeringChangeByIdOrThrow(engineeringChangeId);
            engineeringChangeLabelRepository.saveAll(toAdd.stream().map(engineeringChange::linkLabel).toList());
        }

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Map<UUID, Label> labels = findLabels(Set.copyOf(union(toAdd, toRemove)));
            addDiffActivity(
                    engineeringChangeId,
                    actorId,
                    ACTION_LABEL_CHANGED,
                    toAdd.stream().map(labelId -> toLabelRef(labelId, labels.get(labelId))).toList(),
                    toRemove.stream().map(labelId -> toLabelRef(labelId, labels.get(labelId))).toList()
            );
        }

        return new DiffResult(toAdd, toRemove);
    }

    // ── Affected Item Activity ──

    public void recordAffectedItemDiffActivity(
            UUID actorId,
            UUID engineeringChangeId,
            List<EngineeringChangeAffectedItem> added,
            List<EngineeringChangeAffectedItem> removed
    ) {
        if ((added == null || added.isEmpty()) && (removed == null || removed.isEmpty())) {
            return;
        }
        addDiffActivity(
                engineeringChangeId,
                actorId,
                ACTION_ENGINEERING_CHANGE_PART_REVISION_CHANGED,
                added == null ? List.of() : added.stream().map(this::toAffectedItemRef).toList(),
                removed == null ? List.of() : removed.stream().map(this::toAffectedItemRef).toList()
        );
    }

    // ── Comment ──

    public AbstractComment createComment(UUID actorId, UUID engineeringChangeId, JsonNode body) {
        tipTapValidator.validateDocument(body);
        MentionSource source = getMentionSourceOrThrow(engineeringChangeId);
        EngineeringChange engineeringChange = getEngineeringChangeByIdOrThrow(engineeringChangeId);
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

    // ── File ──

    public List<File> attachFiles(UUID actorId, UUID engineeringChangeId, List<File> files) {
        return attachFiles(actorId, engineeringChangeId, files, true);
    }

    public List<File> attachFiles(UUID actorId, UUID engineeringChangeId, List<File> files, boolean emitActivity) {
        assertMetadataEditable(getEngineeringChangeByIdOrThrow(engineeringChangeId));
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
        getEngineeringChangeByIdOrThrow(engineeringChangeId);

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

    // ── Guard ──

    public void assertMetadataEditable(EngineeringChange ec) {
        if (ec.getState() != EngineeringChangeState.DRAFT) {
            throw new AppException(ErrorCode.INVALID_STATE, "DRAFT 상태의 변경안만 수정할 수 있습니다");
        }
    }

    public void assertContentEditable(EngineeringChange ec) {
        if (ec.getState() != EngineeringChangeState.DRAFT && !ec.hasChangesRequestedStep()) {
            throw new AppException(ErrorCode.INVALID_STATE, "DRAFT 상태이거나 수정 요청이 있는 변경안만 수정할 수 있습니다");
        }
    }

    // ── WorkItem Number ──

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

        workItemNumberSequenceRepository.insertIfAbsent(WORK_ITEM_NUMBER_SEQUENCE_ID, nextNumber);
        return workItemNumberSequenceRepository.findByIdForUpdate(WORK_ITEM_NUMBER_SEQUENCE_ID)
                .orElseThrow(() -> new AppException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "워크아이템 번호 시퀀스를 초기화할 수 없습니다"
                ));
    }

    // ── Step 찾기 (private) ──

    private EngineeringChangeStep findActionableStep(UUID actorId, EngineeringChange ec, UUID stepId) {
        EngineeringChangeStep step = ec.getSteps().stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "단계를 찾을 수 없습니다"));

        if (!step.isPending()) {
            throw new AppException(ErrorCode.INVALID_STATE, "대기 중인 단계만 처리할 수 있습니다");
        }

        // USER 직접 할당 확인
        if (step.isAssignedToUser(actorId)) {
            return step;
        }

        // TEAM 소속 확인
        if (step.getAssigneeType() == EngineeringChangeStepAssigneeType.TEAM) {
            boolean isMember = teamMemberRepository.findByTeam_IdIn(Set.of(step.getAssigneeId())).stream()
                    .anyMatch(member -> actorId.equals(member.getUserId()));
            if (isMember) {
                return step;
            }
        }

        throw new AppException(ErrorCode.FORBIDDEN, "해당 단계의 담당자만 처리할 수 있습니다");
    }

    private void validateAssignee(StepAssigneeDraft assignee) {
        if (assignee.assigneeType() == EngineeringChangeStepAssigneeType.USER) {
            userRepository.findById(assignee.assigneeId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "단계 담당 사용자를 찾을 수 없습니다"));
            return;
        }
        if (!teamRepository.existsById(assignee.assigneeId())) {
            throw new AppException(ErrorCode.NOT_FOUND, "단계 담당 팀을 찾을 수 없습니다");
        }
    }

    private void validateLabels(Iterable<UUID> labelIds) {
        Set<UUID> foundIds = new LinkedHashSet<>();
        for (Label label : labelRepository.findAllById(labelIds)) {
            foundIds.add(label.getId());
        }
        for (UUID labelId : labelIds) {
            if (!foundIds.contains(labelId)) {
                throw new AppException(ErrorCode.NOT_FOUND, "Label '" + labelId + "'을(를) 찾을 수 없습니다");
            }
        }
    }

    private void validateStageDrafts(List<StageDraft> stageDrafts) {
        Set<UUID> stageIds = new LinkedHashSet<>();
        Set<StageKey> stageKeys = new LinkedHashSet<>();
        for (StageDraft draft : stageDrafts) {
            if (draft.stepStageId() != null && !stageIds.add(draft.stepStageId())) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "중복된 stage id는 허용되지 않습니다");
            }
            StageKey stageKey = new StageKey(draft.stepType(), draft.sequence());
            if (!stageKeys.add(stageKey)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "동일한 단계 타입/순서는 중복될 수 없습니다");
            }
            Set<StepAssignmentKey> assignees = new LinkedHashSet<>();
            for (StepAssigneeDraft assignee : draft.assignees()) {
                StepAssignmentKey key = new StepAssignmentKey(assignee.assigneeType(), assignee.assigneeId());
                if (!assignees.add(key)) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "같은 단계 안에 중복 담당자를 지정할 수 없습니다");
                }
            }
        }
    }

    private StepStage resolveStageForSync(
            StageDraft draft,
            Map<UUID, StepStage> existingStagesById,
            Map<StageKey, StepStage> existingStagesByKey,
            Set<UUID> retainedStageIds
    ) {
        if (draft.stepStageId() != null) {
            StepStage stage = existingStagesById.get(draft.stepStageId());
            if (stage == null) {
                throw new AppException(ErrorCode.NOT_FOUND, "단계를 찾을 수 없습니다: " + draft.stepStageId());
            }
            if (retainedStageIds.contains(stage.getId())) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "동일한 단계를 중복 수정할 수 없습니다");
            }
            return stage;
        }

        StepStage stage = existingStagesByKey.get(new StageKey(draft.stepType(), draft.sequence()));
        if (stage == null || retainedStageIds.contains(stage.getId())) {
            return null;
        }
        return stage;
    }

    private void syncStageAssignees(UUID actorId, EngineeringChange ec, StepStage stage, List<StepAssigneeDraft> assigneeDrafts) {
        List<EngineeringChangeStep> stageSteps = ec.getSteps().stream()
                .filter(step -> step.getStepStageId().equals(stage.getId()))
                .toList();
        Map<StepAssignmentKey, EngineeringChangeStep> existingSteps = stageSteps.stream()
                .collect(java.util.stream.Collectors.toMap(
                        step -> new StepAssignmentKey(step.getAssigneeType(), step.getAssigneeId()),
                        step -> step,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Set<StepAssignmentKey> desiredKeys = assigneeDrafts.stream()
                .peek(this::validateAssignee)
                .map(assignee -> new StepAssignmentKey(assignee.assigneeType(), assignee.assigneeId()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<EngineeringChangeStep> stepsToRemove = stageSteps.stream()
                .filter(step -> !desiredKeys.contains(new StepAssignmentKey(step.getAssigneeType(), step.getAssigneeId())))
                .toList();
        if (!stepsToRemove.isEmpty()) {
            engineeringChangeStepRepository.deleteAll(stepsToRemove);
            engineeringChangeStepRepository.flush();
            stepsToRemove.forEach(step -> ec.removeStep(step.getId(), actorId));
        }

        for (StepAssigneeDraft assigneeDraft : assigneeDrafts) {
            StepAssignmentKey key = new StepAssignmentKey(assigneeDraft.assigneeType(), assigneeDraft.assigneeId());
            if (existingSteps.containsKey(key)) {
                continue;
            }
            ec.addStep(stage, assigneeDraft.assigneeType(), assigneeDraft.assigneeId(), actorId);
        }
    }

    private void removeObsoleteStages(UUID actorId, EngineeringChange ec, Set<UUID> retainedStageIds) {
        List<StepStage> stagesToRemove = ec.getStages().stream()
                .filter(stage -> !retainedStageIds.contains(stage.getId()))
                .toList();
        if (stagesToRemove.isEmpty()) {
            return;
        }

        Set<UUID> stageIdsToRemove = stagesToRemove.stream()
                .map(StepStage::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<EngineeringChangeStep> stepsToRemove = ec.getSteps().stream()
                .filter(step -> stageIdsToRemove.contains(step.getStepStageId()))
                .toList();

        if (!stepsToRemove.isEmpty()) {
            engineeringChangeStepRepository.deleteAll(stepsToRemove);
            engineeringChangeStepRepository.flush();
            stepsToRemove.forEach(step -> ec.removeStep(step.getId(), actorId));
        }

        stepStageRepository.deleteAll(stagesToRemove);
        stepStageRepository.flush();
        stagesToRemove.forEach(stage -> ec.removeStage(stage.getId(), actorId));
    }

    // ── Activity 헬퍼 ──

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

    private void addStepActivity(
            UUID ecId,
            UUID actorId,
            ActivityAction action,
            EngineeringChangeStep step,
            String comment
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("stepStageId", step.getStepStageId().toString());
        detail.put("stepId", step.getId().toString());
        if (comment != null) {
            detail.put("comment", comment);
        }
        detail.put("previousStatus", step.getStatus().name());
        detail.put("newStatus", step.getStatus().name());
        addActivity(ecId, actorId, action, detail);
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

    // ── Comment 헬퍼 ──

    private EngineeringChangeComment findCommentOrThrow(UUID engineeringChangeId, UUID commentId) {
        EngineeringChangeComment comment = engineeringChangeCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        if (!comment.getEngineeringChangeId().equals(engineeringChangeId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "해당 변경관리의 댓글이 아닙니다");
        }
        return comment;
    }

    // ── Mention 헬퍼 ──

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

    // ── Ref 변환 헬퍼 ──

    private Map<String, Object> toIssueRef(IssueSnapshot issue) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", issue.id().toString());
        ref.put("type", "issue");
        ref.put("label", "#" + issue.number() + " " + issue.title());
        ref.put("meta", Map.of("number", issue.number(), "state", issue.state()));
        return ref;
    }

    private Map<String, Object> toLabelRef(UUID labelId, Label label) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", labelId.toString());
        ref.put("type", "label");
        ref.put("label", label == null ? "(삭제됨)" : label.getName());
        ref.put("meta", Map.of("color", label == null ? "#888888" : label.getColor()));
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

    private Map<String, Object> toAffectedItemRef(EngineeringChangeAffectedItem item) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", item.getTargetId().toString());
        ref.put("type", item.getItemType().name().toLowerCase());
        ref.put("label", item.getItemType().name() + " / " + item.getTargetId());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("item_type", item.getItemType().name());
        meta.put("action_detail", item.getActionDetail());
        ref.put("meta", meta);
        return ref;
    }

    private Map<UUID, Label> findLabels(Set<UUID> labelIds) {
        if (labelIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Label> labels = new HashMap<>();
        for (Label label : labelRepository.findAllById(labelIds)) {
            labels.put(label.getId(), label);
        }
        return labels;
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

    private Map<String, Object> toUserRef(UUID userId, User user) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", userId.toString());
        ref.put("type", "user");
        ref.put("label", user == null ? "(알 수 없음)" : user.getFullName());
        return ref;
    }

    // ── JSON 헬퍼 ──

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

    // ── 조회 헬퍼 ──

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

    private Map<UUID, String> findTeamNames(Set<UUID> teamIds) {
        if (teamIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> teamNames = new HashMap<>();
        teamRepository.findAllById(teamIds).forEach(team -> teamNames.put(team.getId(), team.getName()));
        return teamNames;
    }

    // ── inner records ──

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

    public record StageDraft(
            UUID stepStageId,
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals,
            Instant deadline,
            List<StepAssigneeDraft> assignees
    ) {
    }

    public record StepAssigneeDraft(
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId
    ) {
    }

    private record StageKey(
            EngineeringChangeStepType stepType,
            int sequence
    ) {
    }

    private record StepAssignmentKey(
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId
    ) {
    }
}
