package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnarchiveProjectUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    @Transactional
    public void execute(UUID projectId) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        projectService.unarchiveProject(projectId, auth.userId());
    }
}
