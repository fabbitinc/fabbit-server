package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class ReleaseEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public void execute(ReleaseEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.engineeringChangeNumber());
        boolean readyToRelease = engineeringChangeService.approveReleaseStep(auth.userId(), engineeringChange);
        if (!readyToRelease) {
            return;
        }
        partRevisionWorkflowApi.releaseEngineeringChange(
                auth.userId(),
                engineeringChange.getId(),
                engineeringChange.getNumber(),
                engineeringChange.getTitle()
        );
        engineeringChangeService.completeRelease(auth.userId(), engineeringChange);
    }

    public record ReleaseEngineeringChangeCommand(int engineeringChangeNumber) {
    }
}
