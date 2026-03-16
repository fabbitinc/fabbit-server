package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

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
public class CancelEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public void execute(CancelEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.engineeringChangeNumber());
        partRevisionWorkflowApi.cancelEngineeringChange(
                auth.userId(),
                engineeringChange.getId(),
                engineeringChange.getNumber(),
                engineeringChange.getTitle()
        );
        engineeringChangeService.cancelEngineeringChange(auth.userId(), engineeringChange);
    }

    public record CancelEngineeringChangeCommand(int engineeringChangeNumber) {
    }
}
