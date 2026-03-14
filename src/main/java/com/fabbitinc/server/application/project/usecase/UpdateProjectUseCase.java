package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.application.project.usecase.command.UpdateProjectCommand;
import com.fabbitinc.server.application.project.usecase.result.UpdateProjectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateProjectUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    public UpdateProjectResult execute(UpdateProjectCommand command) {
        var auth = currentAuthProvider.getCurrentAuth();
        return new UpdateProjectResult(projectService.updateProject(
                command.projectId(),
                auth.userId(),
                command.name(),
                command.description()
        ).getId());
    }
}
