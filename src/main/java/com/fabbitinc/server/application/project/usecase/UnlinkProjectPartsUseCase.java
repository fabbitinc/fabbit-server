package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UnlinkProjectPartsUseCase {

    private final AuthTokenParser authTokenParser;
    private final ProjectService projectService;

    @Transactional
    public void execute(String authorizationHeader, UUID projectId, List<UUID> partIds) {
        authTokenParser.requireAuth(authorizationHeader);
        projectService.unlinkParts(projectId, partIds);
    }
}
