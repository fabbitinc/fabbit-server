package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.project.dto.response.LinkPartsResponse;
import com.fabbitinc.server.application.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LinkProjectPartsUseCase {

    private final AuthTokenParser authTokenParser;
    private final ProjectService projectService;

    @Transactional
    public LinkPartsResponse execute(String authorizationHeader, UUID projectId, List<UUID> partIds) {
        authTokenParser.requireAuth(authorizationHeader);
        int linkedCount = projectService.linkParts(projectId, partIds);
        return new LinkPartsResponse(linkedCount);
    }
}
