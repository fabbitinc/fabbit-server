package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.domain.project.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final AuthTokenParser authTokenParser;
    private final ProjectService projectService;

    @Transactional
    public UUID execute(String authorizationHeader, String name, String description) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        Project project = projectService.createProject(auth.userId(), name, description);
        return project.getId();
    }
}
