package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.part.service.input.ReleasePartRevisionInput;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartRevisionResult;
import com.fabbitinc.server.domain.part.model.PartRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ReleasePartRevisionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final PartRevisionService partRevisionService;

    public ReleasePartRevisionResult execute(ReleasePartRevisionCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        partRevisionWorkflowPolicyService.assertDirectModeEnabled();
        PartRevision revision = partRevisionService.releaseRevision(new ReleasePartRevisionInput(
                command.partNumber(),
                command.revisionCode(),
                command.reason()
        ), auth.userId());
        return new ReleasePartRevisionResult(revision.getPartNumber(), revision.getRevisionCode());
    }
}
