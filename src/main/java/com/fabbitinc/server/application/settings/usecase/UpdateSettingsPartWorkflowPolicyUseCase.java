package com.fabbitinc.server.application.settings.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.settings.usecase.command.UpdateSettingsPartWorkflowPolicyCommand;
import com.fabbitinc.server.application.settings.usecase.result.UpdateSettingsPartWorkflowPolicyResult;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateSettingsPartWorkflowPolicyUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;

    public UpdateSettingsPartWorkflowPolicyResult execute(UpdateSettingsPartWorkflowPolicyCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartRevisionWorkflowPolicy policy = partRevisionWorkflowPolicyService.changeMode(command.mode());
        return new UpdateSettingsPartWorkflowPolicyResult(policy.getMode());
    }
}
