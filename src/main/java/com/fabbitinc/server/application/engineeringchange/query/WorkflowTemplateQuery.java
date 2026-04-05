package com.fabbitinc.server.application.engineeringchange.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.query.result.WorkflowTemplateResult;
import com.fabbitinc.server.domain.engineeringchange.model.WorkflowTemplate;
import com.fabbitinc.server.domain.engineeringchange.model.WorkflowTemplateStage;
import com.fabbitinc.server.domain.engineeringchange.model.WorkflowTemplateStageAssignee;
import com.fabbitinc.server.domain.engineeringchange.repository.WorkflowTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkflowTemplateQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final WorkflowTemplateRepository workflowTemplateRepository;

    public List<WorkflowTemplateResult> listWorkflowTemplates() {
        currentAuthProvider.getCurrentAuth();

        return workflowTemplateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResult)
                .toList();
    }

    private WorkflowTemplateResult toResult(WorkflowTemplate template) {
        return new WorkflowTemplateResult(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getStages().stream().map(this::toStageResult).toList(),
                template.getCreatedAt()
        );
    }

    private WorkflowTemplateResult.StageResult toStageResult(WorkflowTemplateStage stage) {
        return new WorkflowTemplateResult.StageResult(
                stage.getId(),
                stage.getStepType(),
                stage.getSequence(),
                stage.getCompletionPolicy(),
                stage.getMinApprovals(),
                stage.getAssignees().stream().map(this::toAssigneeResult).toList()
        );
    }

    private WorkflowTemplateResult.AssigneeResult toAssigneeResult(WorkflowTemplateStageAssignee assignee) {
        return new WorkflowTemplateResult.AssigneeResult(
                assignee.getAssigneeId(),
                assignee.getAssigneeType()
        );
    }
}
