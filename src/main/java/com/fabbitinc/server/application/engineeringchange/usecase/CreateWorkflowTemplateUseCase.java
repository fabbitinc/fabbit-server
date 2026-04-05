package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import com.fabbitinc.server.domain.engineeringchange.model.WorkflowTemplate;
import com.fabbitinc.server.domain.engineeringchange.model.WorkflowTemplateStage;
import com.fabbitinc.server.domain.engineeringchange.repository.WorkflowTemplateRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateWorkflowTemplateUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final WorkflowTemplateRepository workflowTemplateRepository;

    public CreateWorkflowTemplateResult execute(CreateWorkflowTemplateCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        WorkflowTemplate template = WorkflowTemplate.create(
                command.name(), command.description(), auth.userId()
        );

        for (CreateWorkflowTemplateCommand.StageItem stageItem : command.stages()) {
            WorkflowTemplateStage stage = template.addStage(
                    stageItem.stepType(),
                    stageItem.sequence(),
                    stageItem.completionPolicy(),
                    stageItem.minApprovals()
            );
            for (CreateWorkflowTemplateCommand.AssigneeItem assigneeItem : stageItem.assignees()) {
                stage.addAssignee(assigneeItem.assigneeType(), assigneeItem.assigneeId());
            }
        }

        workflowTemplateRepository.save(template);

        return new CreateWorkflowTemplateResult(template.getId());
    }

    public record CreateWorkflowTemplateCommand(
            String name,
            String description,
            List<StageItem> stages
    ) {
        public CreateWorkflowTemplateCommand {
            stages = stages == null ? List.of() : List.copyOf(stages);
        }

        public record StageItem(
                EngineeringChangeStepType stepType,
                int sequence,
                StepStageCompletionPolicy completionPolicy,
                Integer minApprovals,
                List<AssigneeItem> assignees
        ) {
            public StageItem {
                assignees = assignees == null ? List.of() : List.copyOf(assignees);
            }
        }

        public record AssigneeItem(
                EngineeringChangeStepAssigneeType assigneeType,
                UUID assigneeId
        ) {
        }
    }

    public record CreateWorkflowTemplateResult(UUID workflowTemplateId) {
    }
}
