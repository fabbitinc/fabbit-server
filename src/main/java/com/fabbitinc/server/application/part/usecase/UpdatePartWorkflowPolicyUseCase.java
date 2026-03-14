package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartWorkflowPolicyCommand;
import com.fabbitinc.server.application.part.usecase.result.UpdatePartWorkflowPolicyResult;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdatePartWorkflowPolicyUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;

    public UpdatePartWorkflowPolicyResult execute(UpdatePartWorkflowPolicyCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartRevisionWorkflowPolicy policy = partRevisionWorkflowPolicyService.changeMode(command.mode());
        return new UpdatePartWorkflowPolicyResult(policy.getMode());
    }
}
