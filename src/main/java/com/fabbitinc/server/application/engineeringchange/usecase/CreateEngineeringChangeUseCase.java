package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final EngineeringChangeService engineeringChangeService;
    private final SyncEngineeringChangeAffectedItemsUseCase syncAffectedItemsUseCase;
    private final ObjectMapper objectMapper;

    public CreateEngineeringChangeResult execute(CreateEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        List<UUID> linkedIssueIds = resolveLinkedIssueIds(command.sourceIssueId(), command.linkedIssueIds());

        EngineeringChange engineeringChange =
                engineeringChangeService.createEngineeringChange(
                        auth.userId(),
                        command.title(),
                        command.body(),
                        command.sourceIssueId() != null
                                ? command.sourceIssueId()
                                : linkedIssueIds.isEmpty() ? null : linkedIssueIds.getFirst()
                );

        if (!linkedIssueIds.isEmpty()) {
            engineeringChangeService.syncIssues(
                    auth.userId(),
                    engineeringChange.getId(),
                    linkedIssueIds,
                    false
            );
        }
        if (!command.affectedItems().isEmpty()) {
            syncAffectedItemsUseCase.execute(
                    new SyncEngineeringChangeAffectedItemsUseCase.SyncEngineeringChangeAffectedItemsCommand(
                            engineeringChange.getId(),
                            command.affectedItems().stream()
                                    .map(item -> new SyncEngineeringChangeAffectedItemsUseCase.Item(
                                            item.itemType(),
                                            item.targetId(),
                                            item.targetState()
                                    ))
                                    .toList()
                    )
            );
        }
        if (!command.labelIds().isEmpty()) {
            engineeringChangeService.syncLabels(auth.userId(), engineeringChange.getId(), command.labelIds(), false);
        }
        if (!command.fileIds().isEmpty()) {
            engineeringChangeService.attachFiles(
                    auth.userId(),
                    engineeringChange.getId(),
                    fileService.validateAttachable(command.fileIds()),
                    false
            );
        }
        if (!command.stages().isEmpty()) {
            engineeringChangeService.syncStages(
                    auth.userId(),
                    engineeringChange,
                    command.stages().stream()
                            .map(stage -> new EngineeringChangeService.StageDraft(
                                    null,
                                    stage.stepType(),
                                    stage.sequence(),
                                    stage.completionPolicy(),
                                    stage.minApprovals(),
                                    stage.deadline(),
                                    stage.assignees().stream()
                                            .map(a -> new EngineeringChangeService.StepAssigneeDraft(
                                                    a.assigneeType(),
                                                    a.assigneeId()
                                            ))
                                            .toList()
                            ))
                            .toList()
            );
        }

        return new CreateEngineeringChangeResult(engineeringChange.getId());
    }

    public record CreateEngineeringChangeCommand(
            String title,
            JsonNode body,
            UUID sourceIssueId,
            List<UUID> linkedIssueIds,
            List<AffectedItemTarget> affectedItems,
            List<UUID> labelIds,
            List<UUID> fileIds,
            List<StageTarget> stages
    ) {
        public CreateEngineeringChangeCommand {
            linkedIssueIds = linkedIssueIds == null ? List.of() : List.copyOf(linkedIssueIds);
            affectedItems = affectedItems == null ? List.of() : List.copyOf(affectedItems);
            labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
            fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
            stages = stages == null ? List.of() : List.copyOf(stages);
        }

        public record AffectedItemTarget(
                EngineeringChangeAffectedItemType itemType,
                UUID targetId,
                PartLifecycleState targetState
        ) {
        }

        public record StageTarget(
                EngineeringChangeStepType stepType,
                int sequence,
                StepStageCompletionPolicy completionPolicy,
                Integer minApprovals,
                Instant deadline,
                List<AssigneeTarget> assignees
        ) {
            public StageTarget {
                assignees = assignees == null ? List.of() : List.copyOf(assignees);
            }
        }

        public record AssigneeTarget(
                EngineeringChangeStepAssigneeType assigneeType,
                UUID assigneeId
        ) {
        }
    }

    public record CreateEngineeringChangeResult(UUID engineeringChangeId) {
    }

    private List<UUID> resolveLinkedIssueIds(UUID sourceIssueId, List<UUID> linkedIssueIds) {
        java.util.LinkedHashSet<UUID> resolved = new java.util.LinkedHashSet<>();
        if (sourceIssueId != null) {
            resolved.add(sourceIssueId);
        }
        if (linkedIssueIds != null) {
            resolved.addAll(linkedIssueIds);
        }
        return List.copyOf(resolved);
    }
}
