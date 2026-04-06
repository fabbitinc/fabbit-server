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

        EngineeringChange engineeringChange =
                engineeringChangeService.createEngineeringChange(auth.userId(), command.title(), command.body(), command.sourceIssueId());

        if (command.sourceIssueId() != null) {
            engineeringChangeService.syncIssues(
                    auth.userId(),
                    engineeringChange.getId(),
                    java.util.List.of(command.sourceIssueId()),
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
            List<AffectedItemTarget> affectedItems,
            List<UUID> fileIds,
            List<StageTarget> stages
    ) {
        public CreateEngineeringChangeCommand {
            affectedItems = affectedItems == null ? List.of() : List.copyOf(affectedItems);
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
}
