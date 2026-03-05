package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.application.project.usecase.command.CreateProjectCommand;
import com.fabbitinc.server.application.project.usecase.result.CreateProjectResult;
import com.fabbitinc.server.domain.project.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreateProjectUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    public CreateProjectResult execute(CreateProjectCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Project project = projectService.createProject(auth.userId(), command.name(), command.description());
        return new CreateProjectResult(project.getId());
    }
}
