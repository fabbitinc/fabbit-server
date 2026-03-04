package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ArchiveProjectUseCase {

    private final AuthTokenParser authTokenParser;
    private final ProjectService projectService;

    @Transactional
    public void execute(String authorizationHeader, UUID projectId) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        projectService.archiveProject(projectId, auth.userId());
    }
}
