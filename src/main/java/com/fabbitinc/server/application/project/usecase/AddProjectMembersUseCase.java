package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
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

    private final AuthTokenParser authTokenParser;
    private final ProjectService projectService;

    @Transactional
    public ManageMembersResponse execute(
            String authorizationHeader,
            UUID projectId,
            List<UUID> userIds,
            ProjectRole role
    ) {
        authTokenParser.requireAuth(authorizationHeader);
        ProjectRole actualRole = role == null ? ProjectRole.MEMBER : role;
        int count = projectService.addMembers(projectId, userIds, actualRole);
        return new ManageMembersResponse(count);
    }
}
