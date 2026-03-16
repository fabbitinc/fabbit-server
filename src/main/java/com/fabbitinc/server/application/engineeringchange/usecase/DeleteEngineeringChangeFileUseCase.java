package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class DeleteEngineeringChangeFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(DeleteEngineeringChangeFileCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        engineeringChangeService.detachFile(
                auth.userId(),
                WorkItemUseCaseSupport.resolveEngineeringChangeId(engineeringChangeService, command.engineeringChangeNumber()),
                command.fileId()
        );
    }

    public record DeleteEngineeringChangeFileCommand(
            int engineeringChangeNumber,
            UUID fileId
    ) {
    }
}
