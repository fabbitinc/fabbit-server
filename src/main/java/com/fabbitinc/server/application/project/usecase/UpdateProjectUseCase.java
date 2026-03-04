package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final AuthTokenParser authTokenParser;
    private final ProjectService projectService;

    @Transactional
    public UUID execute(String authorizationHeader, UUID projectId, String name, String description) {
        authTokenParser.requireAuth(authorizationHeader);
        return projectService.updateProject(projectId, name, description).getId();
    }
}
