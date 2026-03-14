package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.part.service.input.PartRevisionDecisionInput;
import com.fabbitinc.server.application.part.usecase.command.ApprovePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.result.ApprovePartRevisionResult;
import com.fabbitinc.server.domain.part.model.PartRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ApprovePartRevisionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final PartRevisionService partRevisionService;

    public ApprovePartRevisionResult execute(ApprovePartRevisionCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        partRevisionWorkflowPolicyService.assertDirectModeEnabled();
        PartRevision revision = partRevisionService.approveDraft(new PartRevisionDecisionInput(
                command.partNumber(),
                command.baseRevisionCode(),
                command.draftKey(),
                command.reason()
        ), auth.userId());
        return new ApprovePartRevisionResult(revision.getPartNumber(), revision.getRevisionCode());
    }
}
