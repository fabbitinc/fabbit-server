package com.fabbitinc.server.application.issue.service;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeApi;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeSnapshot;
import com.fabbitinc.server.application.workitem.event.WorkItemUsersMentionedEvent;
import com.fabbitinc.server.application.workitem.support.MentionExtractor;
import com.fabbitinc.server.application.workitem.support.TipTapValidator;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.activity.model.Activity;
import com.fabbitinc.server.domain.activity.model.ActivityTargetType;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.workitem.model.AbstractComment;
import com.fabbitinc.server.domain.issue.model.Issue;
import com.fabbitinc.server.domain.issue.model.IssueAssignee;
import com.fabbitinc.server.domain.issue.model.IssueComment;
import com.fabbitinc.server.domain.issue.model.IssueLabel;
import com.fabbitinc.server.domain.workitem.model.WorkItemNumberSequence;
import com.fabbitinc.server.domain.issue.model.IssuePart;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueTeamAssignee;
import com.fabbitinc.server.domain.issue.repository.IssueAssigneeRepository;
import com.fabbitinc.server.domain.issue.repository.IssueCommentRepository;
import com.fabbitinc.server.domain.issue.repository.IssueLabelRepository;
import com.fabbitinc.server.domain.workitem.repository.WorkItemNumberSequenceRepository;
import com.fabbitinc.server.domain.issue.repository.IssuePartRepository;
import com.fabbitinc.server.domain.issue.repository.IssueRepository;
import com.fabbitinc.server.domain.issue.repository.IssueTeamAssigneeRepository;
import com.fabbitinc.server.domain.label.model.Label;
import com.fabbitinc.server.domain.label.repository.LabelRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.PartRepository;
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
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class IssueService {

    private static final String OWNER_TYPE_ISSUE = "issue";
    private static final UUID WORK_ITEM_NUMBER_SEQUENCE_ID = UUID.fromString("89d98a7b-6b53-4e63-a02a-73f66f606703");

    private static final ActivityAction ACTION_ISSUE_STATE_CHANGED = ActivityAction.ISSUE_STATE_CHANGED;
    private static final ActivityAction ACTION_ASSIGNEE_CHANGED = ActivityAction.ISSUE_ASSIGNEE_CHANGED;
    private static final ActivityAction ACTION_LABEL_CHANGED = ActivityAction.ISSUE_LABEL_CHANGED;
    private static final ActivityAction ACTION_PART_CHANGED = ActivityAction.ISSUE_PART_CHANGED;
    private static final ActivityAction ACTION_FILE_ATTACHED = ActivityAction.ISSUE_FILE_ATTACHED;
    private static final ActivityAction ACTION_FILE_DETACHED = ActivityAction.ISSUE_FILE_DETACHED;
    private static final ActivityAction ACTION_ISSUE_ENGINEERING_CHANGE_CHANGED =
            ActivityAction.ISSUE_ENGINEERING_CHANGE_CHANGED;
    private static final ActivityAction ACTION_ENGINEERING_CHANGE_ISSUE_CHANGED =
            ActivityAction.ENGINEERING_CHANGE_ISSUE_CHANGED;
    private static final ActivityAction ACTION_ISSUE_MENTIONED = ActivityAction.ISSUE_MENTIONED;

    private final IssueRepository issueRepository;
    private final WorkItemNumberSequenceRepository workItemNumberSequenceRepository;
    private final IssueAssigneeRepository issueAssigneeRepository;
    private final IssueTeamAssigneeRepository issueTeamAssigneeRepository;
    private final IssuePartRepository issuePartRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final PartRepository partRepository;
    private final FileRepository fileRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrganizationApi organizationApi;
    private final EngineeringChangeApi engineeringChangeApi;
    private final TipTapValidator tipTapValidator;
    private final MentionExtractor mentionExtractor;
    private final ObjectMapper objectMapper;

    public Issue getIssueByNumberOrThrow(int issueNumber) {
        return issueRepository.findByNumber(issueNumber)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "이슈를 찾을 수 없습니다"));
    }

    public Issue getIssueOrThrow(UUID issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Issue '" + issueId + "'을(를) 찾을 수 없습니다"));
    }

    public Issue createIssue(UUID actorId, String title, JsonNode body) {
        tipTapValidator.validateDocument(body);
        Issue issue = Issue.create(allocateIssueNumber(), title, toBodyString(body), actorId);
        issueRepository.save(issue);

        registerMentions(issue.getId(), actorId, body, null, false, issue.getNumber(), issue.getTitle(), "issue");
        return issue;
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
            registerMentions(issue.getId(), actorId, body, oldBody, false, issue.getNumber(), issue.getTitle(), "issue");
        }
        return issue;
    }

    public Issue closeIssue(UUID actorId, Issue issue) {
        String oldState = issue.getState().name();
        issue.close(Instant.now(), actorId);
        addStateActivity(issue.getId(), actorId, oldState, issue.getState().name());
        return issue;
    }

    public Issue reopenIssue(UUID actorId, Issue issue) {
        String oldState = issue.getState().name();
        issue.reopen(actorId);
        addStateActivity(issue.getId(), actorId, oldState, issue.getState().name());
        return issue;
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
            issueAssigneeRepository.saveAll(toAdd.stream().map(issue::assignUser).toList());
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
            issueTeamAssigneeRepository.saveAll(toAdd.stream().map(issue::assignTeam).toList());

            Set<UUID> overlapUsers = teamMemberRepository.findByTeam_IdIn(toAdd).stream()
                    .map(TeamMember::getUserId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (!overlapUsers.isEmpty()) {
                issueAssigneeRepository.deleteByIssueIdAndUserIdIn(issueId, overlapUsers);
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
            issueLabelRepository.saveAll(toAdd.stream().map(issue::linkLabel).toList());
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
            issuePartRepository.saveAll(toAdd.stream().map(issue::linkPart).toList());
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

    public DiffResult syncLinkedEngineeringChanges(
            UUID actorId,
            UUID issueId,
            List<UUID> engineeringChangeIds,
            boolean emitActivity
    ) {
        EngineeringChangeApi.DiffResult diff =
                engineeringChangeApi.syncEngineeringChangesForIssue(issueId, engineeringChangeIds);
        Set<UUID> toAdd = diff.added();
        Set<UUID> toRemove = diff.removed();

        if (emitActivity && (!toAdd.isEmpty() || !toRemove.isEmpty())) {
            Issue issue = getIssueOrThrow(issueId);
            Map<UUID, EngineeringChangeSnapshot> engineeringChanges =
                    engineeringChangeApi.getEngineeringChangeSnapshotMap(Set.copyOf(union(toAdd, toRemove)));

            addDiffActivity(
                    issueId,
                    actorId,
                    ACTION_ISSUE_ENGINEERING_CHANGE_CHANGED,
                    toAdd.stream().map(changeId -> toEngineeringChangeRef(engineeringChanges.get(changeId))).toList(),
                    toRemove.stream().map(changeId -> toEngineeringChangeRef(engineeringChanges.get(changeId))).toList()
            );

            Map<String, Object> issueRef = toIssueRef(issue);
            for (UUID addedEngineeringChangeId : toAdd) {
                addDiffActivity(
                        addedEngineeringChangeId,
                        actorId,
                        ACTION_ENGINEERING_CHANGE_ISSUE_CHANGED,
                        List.of(issueRef),
                        List.of()
                );
            }
            for (UUID removedEngineeringChangeId : toRemove) {
                addDiffActivity(
                        removedEngineeringChangeId,
                        actorId,
                        ACTION_ENGINEERING_CHANGE_ISSUE_CHANGED,
                        List.of(),
                        List.of(issueRef)
                );
            }
        }

        return new DiffResult(toAdd, toRemove);
    }

    public AbstractComment createComment(UUID actorId, UUID issueId, JsonNode body) {
        tipTapValidator.validateDocument(body);
        MentionSource source = getMentionSourceOrThrow(issueId);
        Issue issue = getIssueOrThrow(issueId);
        IssueComment comment = issue.writeComment(toBodyString(body), actorId);
        issueCommentRepository.save(comment);

        registerMentions(issueId, actorId, body, null, true, source.number(), source.title(), source.type());
        return comment;
    }

    public AbstractComment updateComment(UUID actorId, UUID issueId, UUID commentId, JsonNode body) {
        tipTapValidator.validateDocument(body);
        MentionSource source = getMentionSourceOrThrow(issueId);
        IssueComment comment = findCommentOrThrow(issueId, commentId);
        if (!comment.getCreatedBy().equals(actorId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "본인이 작성한 댓글만 수정할 수 있습니다");
        }

        JsonNode oldBody = parseJson(comment.getBody());
        comment.updateBody(toBodyString(body), actorId);
        registerMentions(issueId, actorId, body, oldBody, true, source.number(), source.title(), source.type());
        return comment;
    }

    public void deleteComment(UUID actorId, UUID issueId, UUID commentId) {
        IssueComment comment = findCommentOrThrow(issueId, commentId);
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
        long totalBytes = files.stream().mapToLong(File::getFileSize).sum();
        if (totalBytes > 0L) {
            organizationApi.consumeStorageForCurrentTenant(totalBytes);
        }

        if (emitActivity) {
            addDiffActivity(issueId, actorId, ACTION_FILE_ATTACHED, files.stream().map(this::toFileRef).toList(), List.of());
        }
        return files;
    }

    public void detachFile(UUID actorId, UUID issueId, UUID fileId) {
        getIssueOrThrow(issueId);

        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(fileId, OWNER_TYPE_ISSUE, issueId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "해당 이슈에 연결된 파일을 찾을 수 없습니다"));

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
        addDiffActivity(issueId, actorId, ACTION_FILE_DETACHED, List.of(), List.of(removed));
    }

    private int allocateIssueNumber() {
        WorkItemNumberSequence sequence = workItemNumberSequenceRepository.findByIdForUpdate(WORK_ITEM_NUMBER_SEQUENCE_ID)
                .orElseGet(this::initializeWorkItemNumberSequence);
        return sequence.allocateNextNumber();
    }

    private WorkItemNumberSequence initializeWorkItemNumberSequence() {
        int nextIssueNumber = issueRepository.findTopByOrderByNumberDesc()
                .map(issue -> issue.getNumber() + 1)
                .orElse(1);
        int nextEngineeringChangeNumber = engineeringChangeApi.getNextEngineeringChangeNumberSeed();
        int nextNumber = Math.max(nextIssueNumber, nextEngineeringChangeNumber);

        workItemNumberSequenceRepository.insertIfAbsent(WORK_ITEM_NUMBER_SEQUENCE_ID, nextNumber);
        return workItemNumberSequenceRepository.findByIdForUpdate(WORK_ITEM_NUMBER_SEQUENCE_ID)
                .orElseThrow(() -> new AppException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "워크아이템 번호 시퀀스를 초기화할 수 없습니다"
                ));
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

    private void addStateActivity(UUID issueId, UUID actorId, String oldState, String newState) {
        addActivity(
                issueId,
                actorId,
                ACTION_ISSUE_STATE_CHANGED,
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
        if (issueRepository.existsById(targetId)) {
            return ActivityTargetType.ISSUE;
        }
        if (engineeringChangeApi.existsEngineeringChange(targetId)) {
            return ActivityTargetType.ENGINEERING_CHANGE;
        }
        return ActivityTargetType.ISSUE;
    }

    private IssueComment findCommentOrThrow(UUID issueId, UUID commentId) {
        IssueComment comment = issueCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        if (!comment.getIssueId().equals(issueId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "해당 이슈의 댓글이 아닙니다");
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

    private MentionSource getMentionSourceOrThrow(UUID issueId) {
        Issue issue = issueRepository.findById(issueId).orElse(null);
        if (issue == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "대상을 찾을 수 없습니다");
        }
        return new MentionSource(issue.getId(), issue.getNumber(), issue.getTitle(), "issue");
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

    private Map<UUID, Part> findParts(Set<UUID> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Part> parts = new HashMap<>();
        for (Part part : partRepository.findAllById(partIds)) {
            parts.put(part.getId(), part);
        }
        return parts;
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

    private Map<String, Object> toLabelRef(UUID labelId, Label label) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", labelId.toString());
        ref.put("type", "label");
        ref.put("label", label == null ? "(삭제됨)" : label.getName());
        ref.put("meta", Map.of("color", label == null ? "#888888" : label.getColor()));
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
        ref.put("type", "issue");
        ref.put("label", issue == null ? "(알 수 없음)" : "#" + issue.getNumber() + " " + issue.getTitle());
        ref.put("meta", Map.of("number", issue == null ? 0 : issue.getNumber()));
        return ref;
    }

    private Map<String, Object> toEngineeringChangeRef(EngineeringChangeSnapshot engineeringChange) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("id", engineeringChange == null ? "" : engineeringChange.id().toString());
        ref.put("type", "engineering_change");
        ref.put(
                "label",
                engineeringChange == null ? "(알 수 없음)" : "#" + engineeringChange.number() + " " + engineeringChange.title()
        );
        ref.put("meta", Map.of("number", engineeringChange == null ? 0 : engineeringChange.number()));
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
