package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.part.service.input.PartRevisionDecisionInput;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartDraftResult;
import com.fabbitinc.server.domain.part.model.PartRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ReleasePartDraftUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final PartRevisionService partRevisionService;

    public ReleasePartDraftResult execute(ReleasePartDraftCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        partRevisionWorkflowPolicyService.assertDirectModeEnabled();
        PartRevision revision = partRevisionService.releaseDraft(new PartRevisionDecisionInput(
                command.partNumber(),
                command.baseRevisionCode(),
                command.draftKey(),
                command.reason()
        ), auth.userId());
        return new ReleasePartDraftResult(revision.getPartNumber(), revision.getRevisionCode());
    }
}
