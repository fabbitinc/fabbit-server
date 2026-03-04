package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    @Transactional
    public UUID execute(UUID projectId, String name, String description) {
        currentAuthProvider.getCurrentAuth();
        return projectService.updateProject(projectId, name, description).getId();
    }
}
