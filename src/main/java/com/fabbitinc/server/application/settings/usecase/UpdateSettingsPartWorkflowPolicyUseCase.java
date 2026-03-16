package com.fabbitinc.server.application.settings.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.settings.usecase.command.UpdateSettingsPartWorkflowPolicyCommand;
import com.fabbitinc.server.application.settings.usecase.result.UpdateSettingsPartWorkflowPolicyResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowPolicy;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateSettingsPartWorkflowPolicyUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final PartRevisionRepository partRevisionRepository;
    private final EngineeringChangeRepository engineeringChangeRepository;

    public UpdateSettingsPartWorkflowPolicyResult execute(UpdateSettingsPartWorkflowPolicyCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartRevisionWorkflowPolicy currentPolicy = partRevisionWorkflowPolicyService.getCurrent();
        if (currentPolicy.getMode() != command.mode()) {
            assertNoInProgressWorkflow();
        }
        PartRevisionWorkflowPolicy policy = partRevisionWorkflowPolicyService.changeMode(command.mode());
        return new UpdateSettingsPartWorkflowPolicyResult(policy.getMode());
    }

    private void assertNoInProgressWorkflow() {
        boolean hasInProgressRevisions = partRevisionRepository.existsByStatusIn(List.of(
                PartRevisionStatus.DRAFT
        ));
        boolean hasOpenEngineeringChanges = engineeringChangeRepository.existsByStateIn(List.of(
                EngineeringChangeState.DRAFT,
                EngineeringChangeState.REVIEW_PENDING,
                EngineeringChangeState.APPROVAL_PENDING,
                EngineeringChangeState.RELEASE_PENDING
        ));
        if (hasInProgressRevisions || hasOpenEngineeringChanges) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "진행 중인 리비전 또는 EngineeringChange가 있으면 워크플로 모드를 변경할 수 없습니다"
            );
        }
    }
}
