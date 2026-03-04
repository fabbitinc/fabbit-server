package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.dto.response.ManageMembersResponse;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.domain.project.model.ProjectRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddProjectMembersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    @Transactional
    public ManageMembersResponse execute(UUID projectId,
            List<UUID> userIds,
            ProjectRole role
    ) {
        currentAuthProvider.getCurrentAuth();
        ProjectRole actualRole = role == null ? ProjectRole.MEMBER : role;
        int count = projectService.addMembers(projectId, userIds, actualRole);
        return new ManageMembersResponse(count);
    }
}
