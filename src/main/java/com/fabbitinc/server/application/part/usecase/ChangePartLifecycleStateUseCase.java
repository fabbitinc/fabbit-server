package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartLifecycleService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class ChangePartLifecycleStateUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final PartLifecycleService partLifecycleService;

    public ChangePartLifecycleStateResult execute(ChangePartLifecycleStateCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        partRevisionWorkflowPolicyService.assertDirectModeEnabled();
        Part part = partLifecycleService.changeLifecycleState(
                command.partId(), command.targetState(), auth.userId()
        );
        return new ChangePartLifecycleStateResult(part.getId(), part.getLifecycleState());
    }

    public record ChangePartLifecycleStateCommand(UUID partId, PartLifecycleState targetState) {
    }

    public record ChangePartLifecycleStateResult(UUID partId, PartLifecycleState lifecycleState) {
    }
}
