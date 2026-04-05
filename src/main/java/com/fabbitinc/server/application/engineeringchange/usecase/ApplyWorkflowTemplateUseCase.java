package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.WorkflowTemplate;
import com.fabbitinc.server.domain.engineeringchange.repository.WorkflowTemplateRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class ApplyWorkflowTemplateUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final WorkflowTemplateRepository workflowTemplateRepository;

    public ApplyWorkflowTemplateResult execute(ApplyWorkflowTemplateCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());

        WorkflowTemplate template = workflowTemplateRepository.findById(command.workflowTemplateId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "워크플로우 템플릿을 찾을 수 없습니다"));

        engineeringChangeService.replaceStages(
                auth.userId(),
                engineeringChange,
                template.getStages().stream()
                        .map(stage -> new EngineeringChangeService.StageDraft(
                                stage.getStepType(),
                                stage.getSequence(),
                                stage.getCompletionPolicy(),
                                stage.getMinApprovals(),
                                null,
                                stage.getAssignees().stream()
                                        .map(a -> new EngineeringChangeService.StepAssigneeDraft(
                                                a.getAssigneeType(),
                                                a.getAssigneeId()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );

        return new ApplyWorkflowTemplateResult(engineeringChange.getId());
    }

    public record ApplyWorkflowTemplateCommand(
            UUID engineeringChangeId,
            UUID workflowTemplateId
    ) {
    }

    public record ApplyWorkflowTemplateResult(UUID engineeringChangeId) {
    }
}
