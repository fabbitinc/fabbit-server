package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.part.api.EngineeringChangePartRevisionRef;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final IssueApi issueApi;
    private final EngineeringChangeService engineeringChangeService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public CreateEngineeringChangeResult execute(CreateEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange =
                engineeringChangeService.createEngineeringChange(auth.userId(), command.title(), command.body());

        if (command.sourceIssueNumber() != null) {
            engineeringChangeService.syncIssues(
                    auth.userId(),
                    engineeringChange.getId(),
                    java.util.List.of(issueApi.getIssueIdByNumberOrThrow(command.sourceIssueNumber())),
                    false
            );
        }
        if (!command.partRevisions().isEmpty()) {
            partRevisionWorkflowApi.syncEngineeringChangePartRevisions(
                    engineeringChange.getId(),
                    command.partRevisions().stream()
                            .map(item -> new EngineeringChangePartRevisionRef(
                                    item.partNumber(),
                                    item.baseRevisionCode(),
                                    item.draftKey()
                            ))
                            .toList()
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
        if (!command.steps().isEmpty()) {
            engineeringChangeService.replaceSteps(
                    auth.userId(),
                    engineeringChange,
                    command.steps().stream()
                            .map(step -> new EngineeringChangeService.StepDraft(
                                    step.stepType(),
                                    step.assigneeType(),
                                    step.assigneeId(),
                                    step.sequence()
                            ))
                            .toList(),
                    false
            );
        }

        return new CreateEngineeringChangeResult(engineeringChange.getNumber());
    }

    public record CreateEngineeringChangeCommand(
            String title,
            JsonNode body,
            Integer sourceIssueNumber,
            List<PartRevisionTarget> partRevisions,
            List<UUID> fileIds,
            List<StepTarget> steps
    ) {
        public CreateEngineeringChangeCommand {
            partRevisions = partRevisions == null ? List.of() : List.copyOf(partRevisions);
            fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
            steps = steps == null ? List.of() : List.copyOf(steps);
        }

        public record PartRevisionTarget(
                String partNumber,
                String baseRevisionCode,
                String draftKey
        ) {
        }

        public record StepTarget(
                EngineeringChangeStepType stepType,
                EngineeringChangeStepAssigneeType assigneeType,
                UUID assigneeId,
                int sequence
        ) {
        }
    }

    public record CreateEngineeringChangeResult(int engineeringChangeNumber) {
    }
}
