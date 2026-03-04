package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RemoveProjectMembersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    @Transactional
    public void execute(UUID projectId, List<UUID> userIds) {
        currentAuthProvider.getCurrentAuth();
        projectService.removeMembers(projectId, userIds);
    }
}
