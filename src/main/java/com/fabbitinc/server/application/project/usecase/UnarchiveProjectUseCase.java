package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.application.project.usecase.command.UnarchiveProjectCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UnarchiveProjectUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    public void execute(UnarchiveProjectCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        projectService.unarchiveProject(command.projectId(), auth.userId());
    }
}
